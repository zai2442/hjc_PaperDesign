package com.campus.activity.activity.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.activity.activity.dto.RegistrationAuditRequest;
import com.campus.activity.activity.dto.RegistrationStatsResponse;
import com.campus.activity.activity.entity.Activity;
import com.campus.activity.activity.entity.ActivityWhitelist;
import com.campus.activity.activity.entity.Registration;
import com.campus.activity.activity.enums.RegistrationStatus;
import com.campus.activity.activity.mapper.ActivityMapper;
import com.campus.activity.activity.mapper.ActivityWhitelistMapper;
import com.campus.activity.activity.mapper.RegistrationMapper;
import com.campus.activity.activity.service.RegistrationService;
import com.campus.activity.common.ApiException;
import com.campus.activity.common.PageResponse;
import com.campus.activity.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RegistrationServiceImpl extends ServiceImpl<RegistrationMapper, Registration> implements RegistrationService {

    private final RegistrationMapper registrationMapper;
    private final ActivityMapper activityMapper;
    private final ActivityWhitelistMapper whitelistMapper;
    private final StringRedisTemplate stringRedisTemplate;

    private static final String ROLE_SUPER_ADMIN = "ROLE_SUPER_ADMIN";
    private static final String ROLE_COUNSELOR = "ROLE_COUNSELOR";
    private static final String ROLE_CLUB_OWNER = "ROLE_CLUB_OWNER";

    private boolean canManageAny() {
        return SecurityUtils.hasAuthority(ROLE_SUPER_ADMIN)
                || SecurityUtils.hasAuthority(ROLE_COUNSELOR);
    }

    private void assertCanManage(Activity activity) {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            throw new ApiException(401, HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        if (canManageAny()) {
            return;
        }
        if (!activity.getCreatedBy().equals(userId)) {
            throw new ApiException(403, HttpStatus.FORBIDDEN, "Forbidden");
        }
    }

    private static String cacheKeyStock(Long id) {
        return "activity:stock:" + id;
    }

    private static String cacheKeyUserLimit(Long activityId, Long userId) {
        return "activity:user_limit:" + activityId + ":" + userId;
    }

    private static void ensureNotBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new ApiException(400, HttpStatus.BAD_REQUEST, message);
        }
    }

    private void rollbackRedisReserve(String stockKey, String userKey, int limit) {
        stringRedisTemplate.opsForValue().increment(stockKey);
        if (limit > 0) {
            String v = stringRedisTemplate.opsForValue().get(userKey);
            if (v == null) {
                return;
            }
            try {
                long n = Long.parseLong(v);
                if (n <= 1) {
                    stringRedisTemplate.delete(userKey);
                } else {
                    stringRedisTemplate.opsForValue().decrement(userKey);
                }
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    @Transactional
    public Long register(Long activityId, String extraData) {
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new ApiException(404, HttpStatus.NOT_FOUND, "Activity not found");
        }
        if (!"ONLINE".equals(activity.getStatus())) {
            throw new ApiException(400, HttpStatus.BAD_REQUEST, "Activity not online");
        }
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            throw new ApiException(401, HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        if (activity.getWhitelistEnabled() != null && activity.getWhitelistEnabled() == 1) {
            Long cnt = whitelistMapper.selectCount(new LambdaQueryWrapper<ActivityWhitelist>()
                    .eq(ActivityWhitelist::getActivityId, activityId)
                    .eq(ActivityWhitelist::getUserId, userId));
            if (cnt == null || cnt <= 0) {
                throw new ApiException(403, HttpStatus.FORBIDDEN, "Not in whitelist");
            }
        }

        Registration existing = registrationMapper.selectOne(new LambdaQueryWrapper<Registration>()
                .eq(Registration::getActivityId, activityId)
                .eq(Registration::getUserId, userId)
                .last("LIMIT 1"));
        if (existing != null) {
            String status = existing.getStatus();
            if (!RegistrationStatus.CANCELED.name().equals(status) && !RegistrationStatus.REJECTED.name().equals(status)) {
                throw new ApiException(400, HttpStatus.BAD_REQUEST, "Already registered");
            }
        }

        // Redis Lua扣减库存，保证高并发
        String stockKey = cacheKeyStock(activityId);
        String userKey = cacheKeyUserLimit(activityId, userId);
        int limit = activity.getPerUserLimit() == null ? 0 : activity.getPerUserLimit();

        // 确保Redis中有库存
        if (stringRedisTemplate.opsForValue().get(stockKey) == null) {
            stringRedisTemplate.opsForValue().set(stockKey, String.valueOf(activity.getStockAvailable() == null ? 0 : activity.getStockAvailable()));
        }

        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setResultType(Long.class);
        script.setScriptText(
                "local stockKey = KEYS[1]\n" +
                "local userKey = KEYS[2]\n" +
                "local limit = tonumber(ARGV[1])\n" +
                "local stock = tonumber(redis.call('GET', stockKey) or '-1')\n" +
                "if stock <= 0 then return -1 end\n" +
                "local userCount = tonumber(redis.call('GET', userKey) or '0')\n" +
                "if limit > 0 and userCount >= limit then return -2 end\n" +
                "redis.call('DECR', stockKey)\n" +
                "if limit > 0 then redis.call('INCR', userKey) redis.call('EXPIRE', userKey, 2592000) end\n" +
                "return stock - 1\n"
        );
        Long result = stringRedisTemplate.execute(script, List.of(stockKey, userKey), String.valueOf(limit));
        if (result == null || result == -1) {
            throw new ApiException(400, HttpStatus.BAD_REQUEST, "No stock");
        }
        if (result == -2) {
            throw new ApiException(400, HttpStatus.BAD_REQUEST, "Limit reached");
        }

        // 异步或直接更新DB库存。为了简单可靠，这里直接更新DB
        int updatedStock = activityMapper.update(null, new LambdaUpdateWrapper<Activity>()
                .eq(Activity::getId, activityId)
                .gt(Activity::getStockAvailable, 0)
                .setSql("stock_available = stock_available - 1"));
        if (updatedStock <= 0) {
            rollbackRedisReserve(stockKey, userKey, limit);
            throw new ApiException(400, HttpStatus.BAD_REQUEST, "No stock");
        }

        LocalDateTime now = LocalDateTime.now();
        try {
            if (existing != null) {
                existing.setStatus(RegistrationStatus.PENDING.name());
                existing.setExtraData(extraData);
                existing.setAuditReason(null);
                existing.setAuditBy(null);
                existing.setAuditAt(null);
                existing.setUpdatedAt(now);
                registrationMapper.updateById(existing);
                return existing.getId();
            }

            Registration reg = new Registration();
            reg.setActivityId(activityId);
            reg.setUserId(userId);
            reg.setStatus(RegistrationStatus.PENDING.name());
            reg.setExtraData(extraData);
            reg.setCreatedAt(now);
            reg.setUpdatedAt(now);
            registrationMapper.insert(reg);
            return reg.getId();
        } catch (DuplicateKeyException e) {
            activityMapper.update(null, new LambdaUpdateWrapper<Activity>()
                    .eq(Activity::getId, activityId)
                    .setSql("stock_available = stock_available + 1"));
            rollbackRedisReserve(stockKey, userKey, limit);
            throw new ApiException(400, HttpStatus.BAD_REQUEST, "Already registered");
        } catch (RuntimeException e) {
            activityMapper.update(null, new LambdaUpdateWrapper<Activity>()
                    .eq(Activity::getId, activityId)
                    .setSql("stock_available = stock_available + 1"));
            rollbackRedisReserve(stockKey, userKey, limit);
            throw e;
        }
    }

    @Override
    @Transactional
    public void cancel(Long id) {
        Long userId = SecurityUtils.getUserId();
        Registration reg = registrationMapper.selectById(id);
        if (reg == null || !reg.getUserId().equals(userId)) {
            throw new ApiException(404, HttpStatus.NOT_FOUND, "Registration not found");
        }
        if (!RegistrationStatus.PENDING.name().equals(reg.getStatus()) && !RegistrationStatus.APPROVED.name().equals(reg.getStatus())) {
            throw new ApiException(400, HttpStatus.BAD_REQUEST, "Cannot cancel in current status");
        }

        reg.setStatus(RegistrationStatus.CANCELED.name());
        reg.setUpdatedAt(LocalDateTime.now());
        registrationMapper.updateById(reg);

        // 恢复库存
        activityMapper.update(null, new LambdaUpdateWrapper<Activity>()
                .eq(Activity::getId, reg.getActivityId())
                .setSql("stock_available = stock_available + 1"));
        stringRedisTemplate.opsForValue().increment(cacheKeyStock(reg.getActivityId()));

        int limit = 0;
        Activity activity = activityMapper.selectById(reg.getActivityId());
        if (activity != null && activity.getPerUserLimit() != null) {
            limit = activity.getPerUserLimit();
        }
        if (limit > 0) {
            String userKey = cacheKeyUserLimit(reg.getActivityId(), userId);
            String v = stringRedisTemplate.opsForValue().get(userKey);
            if (v != null) {
                try {
                    long n = Long.parseLong(v);
                    if (n <= 1) {
                        stringRedisTemplate.delete(userKey);
                    } else {
                        stringRedisTemplate.opsForValue().decrement(userKey);
                    }
                } catch (Exception ignored) {
                }
            }
        }
    }

    @Override
    public PageResponse<Registration> getMyRegistrations(long page, long size, String status) {
        Long userId = SecurityUtils.getUserId();
        long p = Math.max(1, page);
        long s = Math.min(200, Math.max(1, size));
        Page<Registration> mpPage = new Page<>(p, s);
        LambdaQueryWrapper<Registration> qw = new LambdaQueryWrapper<>();
        qw.eq(Registration::getUserId, userId);
        if (status != null && !status.isBlank()) {
            qw.eq(Registration::getStatus, status);
        }
        qw.orderByDesc(Registration::getId);
        Page<Registration> result = registrationMapper.selectPage(mpPage, qw);
        return PageResponse.of(p, s, result.getTotal(), result.getRecords());
    }

    @Override
    public Registration getDetail(Long id) {
        Registration reg = registrationMapper.selectById(id);
        if (reg == null) {
            throw new ApiException(404, HttpStatus.NOT_FOUND, "Registration not found");
        }
        Long userId = SecurityUtils.getUserId();
        if (userId != null && userId.equals(reg.getUserId())) {
            return reg;
        }
        Activity activity = activityMapper.selectById(reg.getActivityId());
        if (activity == null) {
            throw new ApiException(404, HttpStatus.NOT_FOUND, "Activity not found");
        }
        if (canManageAny() || (userId != null && userId.equals(activity.getCreatedBy()))) {
            return reg;
        }
        throw new ApiException(403, HttpStatus.FORBIDDEN, "Forbidden");
    }

    @Override
    public PageResponse<Registration> getAdminRegistrations(Long activityId, String status, String keyword, long page, long size) {
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new ApiException(404, HttpStatus.NOT_FOUND, "Activity not found");
        }
        assertCanManage(activity);

        long p = Math.max(1, page);
        long s = Math.min(200, Math.max(1, size));
        Page<Registration> mpPage = new Page<>(p, s);
        LambdaQueryWrapper<Registration> qw = new LambdaQueryWrapper<>();
        qw.eq(Registration::getActivityId, activityId);
        if (status != null && !status.isBlank()) {
            qw.eq(Registration::getStatus, status);
        }
        // 如果有keyword，通常是搜索用户名，但当前表没有用户名。为简化可忽略keyword，或连表查询
        qw.orderByDesc(Registration::getId);
        Page<Registration> result = registrationMapper.selectPage(mpPage, qw);
        return PageResponse.of(p, s, result.getTotal(), result.getRecords());
    }

    @Override
    @Transactional
    public void audit(Long id, RegistrationAuditRequest request) {
        Registration reg = registrationMapper.selectById(id);
        if (reg == null) {
            throw new ApiException(404, HttpStatus.NOT_FOUND, "Registration not found");
        }
        Activity activity = activityMapper.selectById(reg.getActivityId());
        assertCanManage(activity);

        if (!RegistrationStatus.PENDING.name().equals(reg.getStatus())) {
            throw new ApiException(400, HttpStatus.BAD_REQUEST, "Can only audit PENDING registrations");
        }

        ensureNotBlank(request.getStatus(), "Invalid audit status");
        Long auditorId = SecurityUtils.getUserId();
        LocalDateTime now = LocalDateTime.now();
        reg.setAuditReason(request.getReason());
        reg.setAuditBy(auditorId);
        reg.setAuditAt(now);
        if (RegistrationStatus.APPROVED.name().equals(request.getStatus())) {
            reg.setStatus(RegistrationStatus.APPROVED.name());
        } else if (RegistrationStatus.REJECTED.name().equals(request.getStatus())) {
            reg.setStatus(RegistrationStatus.REJECTED.name());
            // 恢复库存
            activityMapper.update(null, new LambdaUpdateWrapper<Activity>()
                    .eq(Activity::getId, reg.getActivityId())
                    .setSql("stock_available = stock_available + 1"));
            stringRedisTemplate.opsForValue().increment(cacheKeyStock(reg.getActivityId()));

            int limit = activity.getPerUserLimit() == null ? 0 : activity.getPerUserLimit();
            if (limit > 0) {
                String userKey = cacheKeyUserLimit(reg.getActivityId(), reg.getUserId());
                String v = stringRedisTemplate.opsForValue().get(userKey);
                if (v != null) {
                    try {
                        long n = Long.parseLong(v);
                        if (n <= 1) {
                            stringRedisTemplate.delete(userKey);
                        } else {
                            stringRedisTemplate.opsForValue().decrement(userKey);
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        } else {
            throw new ApiException(400, HttpStatus.BAD_REQUEST, "Invalid audit status");
        }
        reg.setUpdatedAt(now);
        registrationMapper.updateById(reg);
    }

    @Override
    public RegistrationStatsResponse getStats(Long activityId) {
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new ApiException(404, HttpStatus.NOT_FOUND, "Activity not found");
        }
        assertCanManage(activity);

        List<Map<String, Object>> stats = registrationMapper.selectMaps(new QueryWrapper<Registration>()
                .select("status, COUNT(*) AS cnt")
                .eq("activity_id", activityId)
                .groupBy("status"));

        RegistrationStatsResponse response = new RegistrationStatsResponse();
        response.setActivityId(activityId);
        long total = 0, pending = 0, approved = 0, rejected = 0, canceled = 0, completed = 0;

        for (Map<String, Object> stat : stats) {
            long cnt = 0;
            Object c = stat.get("cnt");
            if (c == null) {
                c = stat.get("CNT");
            }
            if (c instanceof Number) {
                cnt = ((Number) c).longValue();
            } else if (c != null) {
                try {
                    cnt = Long.parseLong(String.valueOf(c));
                } catch (Exception ignored) {
                }
            }
            total += cnt;
            Object s = stat.get("status");
            if (s == null) {
                s = stat.get("STATUS");
            }
            String status = s == null ? null : String.valueOf(s);
            if (RegistrationStatus.PENDING.name().equals(status)) pending += cnt;
            else if (RegistrationStatus.APPROVED.name().equals(status)) approved += cnt;
            else if (RegistrationStatus.REJECTED.name().equals(status)) rejected += cnt;
            else if (RegistrationStatus.CANCELED.name().equals(status)) canceled += cnt;
            else if (RegistrationStatus.COMPLETED.name().equals(status)) completed += cnt;
        }
        response.setTotal(total);
        response.setPending(pending);
        response.setApproved(approved);
        response.setRejected(rejected);
        response.setCanceled(canceled);
        response.setCompleted(completed);

        return response;
    }
}
