package com.campus.activity.activity.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.activity.activity.dto.ActivityAdminListResponse;
import com.campus.activity.activity.dto.ActivityAdminResponse;
import com.campus.activity.activity.dto.ActivityCreateRequest;
import com.campus.activity.activity.dto.ActivityQueryRequest;
import com.campus.activity.activity.dto.ActivityReserveResponse;
import com.campus.activity.activity.dto.ActivityUpdateRequest;
import com.campus.activity.activity.dto.ActivityVariantUpsertRequest;
import com.campus.activity.activity.dto.ActivityWorkflowRequest;
import com.campus.activity.activity.entity.Activity;
import com.campus.activity.activity.entity.ActivityChangeLog;
import com.campus.activity.activity.entity.ActivityVariant;
import com.campus.activity.activity.entity.ActivityWhitelist;
import com.campus.activity.activity.entity.Registration;
import com.campus.activity.activity.entity.Tag;
import com.campus.activity.activity.mapper.ActivityChangeLogMapper;
import com.campus.activity.activity.mapper.ActivityMapper;
import com.campus.activity.activity.mapper.ActivityVariantMapper;
import com.campus.activity.activity.mapper.ActivityWhitelistMapper;
import com.campus.activity.activity.mapper.RegistrationMapper;
import com.campus.activity.activity.mapper.TagMapper;
import com.campus.activity.activity.service.ActivityService;
import com.campus.activity.activity.util.ActivityDiffUtils;
import com.campus.activity.common.ApiException;
import com.campus.activity.common.PageResponse;
import com.campus.activity.log.service.OperationLogService;
import com.campus.activity.security.SecurityUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ActivityServiceImpl extends ServiceImpl<ActivityMapper, Activity> implements ActivityService {

    private static final String ROLE_SUPER_ADMIN = "ROLE_SUPER_ADMIN";
    private static final String ROLE_COUNSELOR = "ROLE_COUNSELOR";
    private static final String ROLE_CLUB_OWNER = "ROLE_CLUB_OWNER";

    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_PENDING_REVIEW = "PENDING_REVIEW";
    private static final String STATUS_REJECTED = "REJECTED";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_ONLINE = "ONLINE";
    private static final String STATUS_OFFLINE = "OFFLINE";

    private final ActivityMapper activityMapper;
    private final ActivityVariantMapper activityVariantMapper;
    private final ActivityChangeLogMapper changeLogMapper;
    private final ActivityWhitelistMapper whitelistMapper;
    private final RegistrationMapper registrationMapper;
    private final TagMapper tagMapper;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final OperationLogService operationLogService;

    private static String cacheKeyDetail(Long id) {
        return "activity:detail:" + id;
    }

    private static String cacheKeyStock(Long id) {
        return "activity:stock:" + id;
    }

    private static String cacheKeyUserLimit(Long activityId, Long userId) {
        return "activity:user_limit:" + activityId + ":" + userId;
    }

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
        if (!Objects.equals(activity.getCreatedBy(), userId)) {
            throw new ApiException(403, HttpStatus.FORBIDDEN, "Forbidden");
        }
    }

    private Activity mustGet(Long id) {
        Activity activity = activityMapper.selectById(id);
        if (activity == null) {
            throw new ApiException(404, HttpStatus.NOT_FOUND, "Activity not found");
        }
        return activity;
    }

    private void writeChangeLog(Long activityId, Long operatorId, String opType, Activity before, Activity after) {
        ActivityChangeLog log = new ActivityChangeLog();
        log.setActivityId(activityId);
        log.setOperatorId(operatorId);
        log.setOpType(opType);
        try {
            log.setBeforeData(objectMapper.writeValueAsString(before));
            log.setAfterData(objectMapper.writeValueAsString(after));
            log.setDiffData(ActivityDiffUtils.diff(objectMapper, before, after));
        } catch (JsonProcessingException e) {
            log.setBeforeData("{}");
            log.setAfterData("{}");
            log.setDiffData("{}");
        }
        log.setCreatedAt(LocalDateTime.now());
        changeLogMapper.insert(log);
    }

    private void writeOpLog(Long activityId, Long operatorId, String opType, String diffData) {
        ActivityChangeLog log = new ActivityChangeLog();
        log.setActivityId(activityId);
        log.setOperatorId(operatorId);
        log.setOpType(opType);
        log.setBeforeData("{}");
        log.setAfterData("{}");
        log.setDiffData(diffData == null ? "{}" : diffData);
        log.setCreatedAt(LocalDateTime.now());
        changeLogMapper.insert(log);
    }

    private void cacheActivityDetail(Activity activity) {
        try {
            stringRedisTemplate.opsForValue().set(cacheKeyDetail(activity.getId()), objectMapper.writeValueAsString(activity));
        } catch (JsonProcessingException ignored) {
        }
    }

    private void evictActivityDetail(Long id) {
        stringRedisTemplate.delete(cacheKeyDetail(id));
    }

    private void initStockToRedisIfAbsent(Activity activity) {
        String stockKey = cacheKeyStock(activity.getId());
        String existing = stringRedisTemplate.opsForValue().get(stockKey);
        if (existing == null) {
            int stock = activity.getStockAvailable() == null ? 0 : activity.getStockAvailable();
            stringRedisTemplate.opsForValue().set(stockKey, String.valueOf(stock));
        }
    }

    @Override
    @Transactional
    public Long create(ActivityCreateRequest req) {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            throw new ApiException(401, HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        if (!(canManageAny() || SecurityUtils.hasAuthority(ROLE_CLUB_OWNER))) {
            throw new ApiException(403, HttpStatus.FORBIDDEN, "Forbidden");
        }

        Activity activity = new Activity();
        activity.setTitle(req.getTitle());
        activity.setSummary(req.getSummary());
        activity.setCoverUrl(req.getCoverUrl());
        activity.setContent(req.getContent());
        activity.setContentType(req.getContentType());
        activity.setLocation(req.getLocation());
        activity.setStartTime(req.getStartTime());
        activity.setEndTime(req.getEndTime());
        activity.setRegStartTime(req.getRegStartTime());
        activity.setRegEndTime(req.getRegEndTime());
        activity.setPublishAt(req.getPublishAt());
        activity.setOfflineAt(req.getOfflineAt());
        activity.setFormSchema(req.getFormSchema());
        activity.setChannels(req.getChannels());
        activity.setWhitelistEnabled(req.getWhitelistEnabled() == null ? 0 : req.getWhitelistEnabled());

        int total = req.getStockTotal() == null ? 0 : Math.max(0, req.getStockTotal());
        activity.setStockTotal(total);
        activity.setStockAvailable(total);
        activity.setPerUserLimit(req.getPerUserLimit() == null ? 0 : Math.max(0, req.getPerUserLimit()));

        activity.setCurrentVariant("A");
        activity.setStatus(STATUS_DRAFT);
        activity.setCreatedBy(userId);
        activity.setUpdatedBy(userId);
        activity.setCreatedAt(LocalDateTime.now());
        activity.setUpdatedAt(LocalDateTime.now());
        activity.setVersion(0);
        activity.setDeleted(0);
        activityMapper.insert(activity);

        if (req.getTagIds() != null && !req.getTagIds().isEmpty()) {
            for (Long tagId : req.getTagIds()) {
                activityMapper.insertActivityTag(activity.getId(), tagId);
            }
        }

        Activity after = mustGet(activity.getId());
        writeChangeLog(after.getId(), userId, "CREATE", after, after);
        operationLogService.log("CREATE", after.getId(), after.getTitle(), after, true, null);
        return activity.getId();
    }

    @Override
    public PageResponse<ActivityAdminListResponse> page(ActivityQueryRequest req) {
        long page = Math.max(1, req.getPage());
        long size = Math.min(200, Math.max(1, req.getSize()));
        LambdaQueryWrapper<Activity> qw = buildAdminQuery(req);

        Page<Activity> mpPage = new Page<>(page, size);
        Page<Activity> result = activityMapper.selectPage(mpPage, qw);
        
        List<ActivityAdminListResponse> records = result.getRecords().stream().map((Activity a) -> {
            List<Tag> tags = tagMapper.findTagsByActivityId(a.getId());
            return ActivityAdminListResponse.from(a, tags);
        }).collect(Collectors.toList());

        return PageResponse.of(page, size, result.getTotal(), records);
    }

    private LambdaQueryWrapper<Activity> buildAdminQuery(ActivityQueryRequest req) {
        LambdaQueryWrapper<Activity> qw = new LambdaQueryWrapper<>();
        if (req.getKeyword() != null && !req.getKeyword().isBlank()) {
            qw.like(Activity::getTitle, req.getKeyword().trim());
        }
        if (req.getStatus() != null && !req.getStatus().isBlank()) {
            qw.eq(Activity::getStatus, req.getStatus().trim());
        }
        if (req.getCreatedBy() != null) {
            qw.eq(Activity::getCreatedBy, req.getCreatedBy());
        }
        if (req.getCreatedFrom() != null) {
            qw.ge(Activity::getCreatedAt, req.getCreatedFrom());
        }
        if (req.getCreatedTo() != null) {
            qw.le(Activity::getCreatedAt, req.getCreatedTo());
        }
        if (!canManageAny()) {
            Long userId = SecurityUtils.getUserId();
            if (userId == null) {
                throw new ApiException(401, HttpStatus.UNAUTHORIZED, "Unauthorized");
            }
            qw.eq(Activity::getCreatedBy, userId);
        }
        qw.orderByDesc(Activity::getId);
        return qw;
    }

    @Override
    public byte[] exportCsv(ActivityQueryRequest req) {
        LambdaQueryWrapper<Activity> qw = buildAdminQuery(req);
        qw.last("limit 5000");
        List<Activity> list = activityMapper.selectList(qw);

        StringBuilder sb = new StringBuilder();
        sb.append("id,title,status,publishAt,offlineAt,stockTotal,stockAvailable,createdBy,createdAt,updatedAt\n");
        for (Activity a : list) {
            sb.append(a.getId()).append(',');
            sb.append(csv(a.getTitle())).append(',');
            sb.append(csv(a.getStatus())).append(',');
            sb.append(csv(a.getPublishAt() == null ? "" : a.getPublishAt().toString())).append(',');
            sb.append(csv(a.getOfflineAt() == null ? "" : a.getOfflineAt().toString())).append(',');
            sb.append(a.getStockTotal() == null ? 0 : a.getStockTotal()).append(',');
            sb.append(a.getStockAvailable() == null ? 0 : a.getStockAvailable()).append(',');
            sb.append(a.getCreatedBy() == null ? "" : a.getCreatedBy()).append(',');
            sb.append(csv(a.getCreatedAt() == null ? "" : a.getCreatedAt().toString())).append(',');
            sb.append(csv(a.getUpdatedAt() == null ? "" : a.getUpdatedAt().toString())).append('\n');
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String csv(String v) {
        if (v == null) {
            return "";
        }
        String s = v.replace("\"", "\"\"");
        if (s.contains(",") || s.contains("\n") || s.contains("\r")) {
            return "\"" + s + "\"";
        }
        return s;
    }

    @Override
    public ActivityAdminResponse getDetailForAdmin(Long id) {
        Activity activity = mustGet(id);
        assertCanManage(activity);
        return ActivityAdminResponse.of(activity, tagMapper.findTagsByActivityId(id));
    }

    private void requireUpdatableStatus(Activity activity) {
        if (STATUS_PENDING_REVIEW.equals(activity.getStatus())) {
            throw new ApiException(400, HttpStatus.BAD_REQUEST, "Activity is pending review");
        }
        if (STATUS_ONLINE.equals(activity.getStatus())) {
            throw new ApiException(400, HttpStatus.BAD_REQUEST, "Online activity cannot be edited directly");
        }
    }

    @Override
    @Transactional
    public void update(Long id, ActivityUpdateRequest req) {
        Activity before = mustGet(id);
        assertCanManage(before);
        requireUpdatableStatus(before);

        Activity patch = new Activity();
        patch.setId(id);
        patch.setVersion(req.getVersion());
        patch.setTitle(req.getTitle());
        patch.setSummary(req.getSummary());
        patch.setCoverUrl(req.getCoverUrl());
        patch.setContent(req.getContent());
        patch.setContentType(req.getContentType());
        patch.setLocation(req.getLocation());
        patch.setStartTime(req.getStartTime());
        patch.setEndTime(req.getEndTime());
        patch.setRegStartTime(req.getRegStartTime());
        patch.setRegEndTime(req.getRegEndTime());
        patch.setFormSchema(req.getFormSchema());
        patch.setChannels(req.getChannels());
        patch.setWhitelistEnabled(req.getWhitelistEnabled());
        patch.setPerUserLimit(req.getPerUserLimit());
        patch.setCurrentVariant(req.getCurrentVariant());
        patch.setPublishAt(req.getPublishAt());
        patch.setOfflineAt(req.getOfflineAt());

        if (req.getStockTotal() != null) {
            int newTotal = Math.max(0, req.getStockTotal());
            int oldTotal = before.getStockTotal() == null ? 0 : before.getStockTotal();
            int oldAvailable = before.getStockAvailable() == null ? 0 : before.getStockAvailable();
            
            // 计算名额变动: 新可用 = 旧可用 + (新总数 - 旧总数)
            int diff = newTotal - oldTotal;
            int newAvailable = Math.max(0, oldAvailable + diff);
            
            patch.setStockTotal(newTotal);
            patch.setStockAvailable(newAvailable);
        }

        Long userId = SecurityUtils.getUserId();
        patch.setUpdatedBy(userId);
        patch.setUpdatedAt(LocalDateTime.now());

        boolean ok = this.updateById(patch);
        if (!ok) {
            throw new ApiException(409, HttpStatus.CONFLICT, "Version conflict");
        }
        
        // 清理缓存 (包括详情和库存)
        evictActivityDetail(id);
        stringRedisTemplate.delete(cacheKeyStock(id));

        if (req.getTagIds() != null) {
            activityMapper.deleteActivityTags(id);
            for (Long tagId : req.getTagIds()) {
                activityMapper.insertActivityTag(id, tagId);
            }
        }

        Activity after = mustGet(id);
        writeChangeLog(id, userId, "UPDATE", before, after);
        operationLogService.log("UPDATE", id, after.getTitle(), after, true, null);
        evictActivityDetail(id);
    }

    @Override
    @Transactional
    public void delete(Long id, Integer version) {
        Activity before = mustGet(id);
        assertCanManage(before);
        if (!canManageAny()) {
            throw new ApiException(403, HttpStatus.FORBIDDEN, "Forbidden");
        }

        LambdaUpdateWrapper<Activity> uw = new LambdaUpdateWrapper<>();
        uw.eq(Activity::getId, id).eq(Activity::getVersion, version).set(Activity::getDeleted, 1);
        boolean ok = this.update(uw);
        if (!ok) {
            throw new ApiException(409, HttpStatus.CONFLICT, "Version conflict");
        }
        Activity after = objectMapper.convertValue(before, Activity.class);
        after.setDeleted(1);
        writeChangeLog(id, SecurityUtils.getUserId(), "DELETE", before, after);
        operationLogService.log("DELETE", id, before.getTitle(), after, true, null);
        evictActivityDetail(id);
    }

    @Override
    @Transactional
    public void submitReview(Long id, ActivityWorkflowRequest req) {
        Activity before = mustGet(id);
        assertCanManage(before);
        if (!STATUS_DRAFT.equals(before.getStatus()) && !STATUS_REJECTED.equals(before.getStatus()) && !STATUS_OFFLINE.equals(before.getStatus())) {
            throw new ApiException(400, HttpStatus.BAD_REQUEST, "Invalid status");
        }

        Activity patch = new Activity();
        patch.setId(id);
        patch.setVersion(req.getVersion());
        patch.setStatus(STATUS_PENDING_REVIEW);
        patch.setUpdatedBy(SecurityUtils.getUserId());
        patch.setUpdatedAt(LocalDateTime.now());
        boolean ok = this.updateById(patch);
        if (!ok) {
            throw new ApiException(409, HttpStatus.CONFLICT, "Version conflict");
        }

        Activity after = mustGet(id);
        writeChangeLog(id, SecurityUtils.getUserId(), "SUBMIT_REVIEW", before, after);
    }

    @Override
    @Transactional
    public void withdraw(Long id, ActivityWorkflowRequest req) {
        Activity before = mustGet(id);
        assertCanManage(before);
        if (!STATUS_PENDING_REVIEW.equals(before.getStatus())) {
            throw new ApiException(400, HttpStatus.BAD_REQUEST, "Invalid status");
        }

        Activity patch = new Activity();
        patch.setId(id);
        patch.setVersion(req.getVersion());
        patch.setStatus(STATUS_DRAFT);
        patch.setUpdatedBy(SecurityUtils.getUserId());
        patch.setUpdatedAt(LocalDateTime.now());
        boolean ok = this.updateById(patch);
        if (!ok) {
            throw new ApiException(409, HttpStatus.CONFLICT, "Version conflict");
        }
        Activity after = mustGet(id);
        writeChangeLog(id, SecurityUtils.getUserId(), "WITHDRAW", before, after);
        operationLogService.log("WITHDRAW", id, after.getTitle(), after, true, null);
    }

    @Override
    @Transactional
    public void approve(Long id, ActivityWorkflowRequest req) {
        Activity before = mustGet(id);
        if (!canManageAny()) {
            throw new ApiException(403, HttpStatus.FORBIDDEN, "Forbidden");
        }
        if (!STATUS_PENDING_REVIEW.equals(before.getStatus())) {
            throw new ApiException(400, HttpStatus.BAD_REQUEST, "Invalid status");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime publishAt = req.getPublishAt() == null ? now : req.getPublishAt();
        LocalDateTime offlineAt = req.getOfflineAt();

        String nextStatus = publishAt.isAfter(now) ? STATUS_APPROVED : STATUS_ONLINE;

        Activity patch = new Activity();
        patch.setId(id);
        patch.setVersion(req.getVersion());
        patch.setStatus(nextStatus);
        patch.setPublishAt(publishAt);
        patch.setOfflineAt(offlineAt);
        patch.setAuditReason(req.getReason());
        patch.setAuditBy(SecurityUtils.getUserId());
        patch.setAuditAt(now);
        patch.setUpdatedBy(SecurityUtils.getUserId());
        patch.setUpdatedAt(now);
        boolean ok = this.updateById(patch);
        if (!ok) {
            throw new ApiException(409, HttpStatus.CONFLICT, "Version conflict");
        }

        Activity after = mustGet(id);
        writeChangeLog(id, SecurityUtils.getUserId(), "APPROVE", before, after);
        operationLogService.log("APPROVE", id, after.getTitle(), after, true, null);

        if (STATUS_ONLINE.equals(after.getStatus())) {
            cacheActivityDetail(after);
            initStockToRedisIfAbsent(after);
        }
    }

    @Override
    @Transactional
    public void reject(Long id, ActivityWorkflowRequest req) {
        Activity before = mustGet(id);
        if (!canManageAny()) {
            throw new ApiException(403, HttpStatus.FORBIDDEN, "Forbidden");
        }
        if (!STATUS_PENDING_REVIEW.equals(before.getStatus())) {
            throw new ApiException(400, HttpStatus.BAD_REQUEST, "Invalid status");
        }

        Activity patch = new Activity();
        patch.setId(id);
        patch.setVersion(req.getVersion());
        patch.setStatus(STATUS_REJECTED);
        patch.setAuditReason(req.getReason());
        patch.setAuditBy(SecurityUtils.getUserId());
        patch.setAuditAt(LocalDateTime.now());
        patch.setUpdatedBy(SecurityUtils.getUserId());
        patch.setUpdatedAt(LocalDateTime.now());
        boolean ok = this.updateById(patch);
        if (!ok) {
            throw new ApiException(409, HttpStatus.CONFLICT, "Version conflict");
        }
        Activity after = mustGet(id);
        writeChangeLog(id, SecurityUtils.getUserId(), "REJECT", before, after);
        operationLogService.log("REJECT", id, after.getTitle(), after, true, null);
    }

    @Override
    @Transactional
    public void revokeSchedule(Long id, ActivityWorkflowRequest req) {
        Activity before = mustGet(id);
        assertCanManage(before);
        if (!STATUS_APPROVED.equals(before.getStatus())) {
            throw new ApiException(400, HttpStatus.BAD_REQUEST, "Invalid status");
        }
        if (before.getPublishAt() == null || !before.getPublishAt().isAfter(LocalDateTime.now())) {
            throw new ApiException(400, HttpStatus.BAD_REQUEST, "Not a scheduled publish");
        }

        Activity patch = new Activity();
        patch.setId(id);
        patch.setVersion(req.getVersion());
        patch.setStatus(STATUS_DRAFT);
        patch.setPublishAt(null);
        patch.setUpdatedBy(SecurityUtils.getUserId());
        patch.setUpdatedAt(LocalDateTime.now());
        boolean ok = this.updateById(patch);
        if (!ok) {
            throw new ApiException(409, HttpStatus.CONFLICT, "Version conflict");
        }
        Activity after = mustGet(id);
        writeChangeLog(id, SecurityUtils.getUserId(), "REVOKE_SCHEDULE", before, after);
        operationLogService.log("REVOKE_SCHEDULE", id, after.getTitle(), after, true, null);
    }

    @Override
    @Transactional
    public void offline(Long id, ActivityWorkflowRequest req) {
        Activity before = mustGet(id);
        assertCanManage(before);
        if (!STATUS_ONLINE.equals(before.getStatus())) {
            throw new ApiException(400, HttpStatus.BAD_REQUEST, "Invalid status");
        }

        Activity patch = new Activity();
        patch.setId(id);
        patch.setVersion(req.getVersion());
        patch.setStatus(STATUS_OFFLINE);
        patch.setUpdatedBy(SecurityUtils.getUserId());
        patch.setUpdatedAt(LocalDateTime.now());
        boolean ok = this.updateById(patch);
        if (!ok) {
            throw new ApiException(409, HttpStatus.CONFLICT, "Version conflict");
        }

        Activity after = mustGet(id);
        writeChangeLog(id, SecurityUtils.getUserId(), "OFFLINE", before, after);
        operationLogService.log("OFFLINE", id, after.getTitle(), after, true, null);
        evictActivityDetail(id);
    }

    @Override
    @Transactional
    public void batchOffline(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return;
        Long userId = SecurityUtils.getUserId();
        LocalDateTime now = LocalDateTime.now();
        for (Long id : ids) {
            Activity activity = activityMapper.selectById(id);
            if (activity == null || !STATUS_ONLINE.equals(activity.getStatus())) continue;
            try {
                assertCanManage(activity);
                Activity patch = new Activity();
                patch.setId(id);
                patch.setStatus(STATUS_OFFLINE);
                patch.setUpdatedBy(userId);
                patch.setUpdatedAt(now);
                activityMapper.updateById(patch);
                evictActivityDetail(id);
                writeOpLog(id, userId, "BATCH_OFFLINE", "{}");
                operationLogService.log("OFFLINE", id, activity.getTitle(), patch, true, null);
            } catch (Exception e) {
                operationLogService.log("OFFLINE", id, null, null, false, e.getMessage());
            }
        }
    }

    @Override
    @Transactional
    public void batchDelete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return;
        if (!canManageAny()) {
            throw new ApiException(403, HttpStatus.FORBIDDEN, "Forbidden");
        }
        Long userId = SecurityUtils.getUserId();
        for (Long id : ids) {
            Activity activity = activityMapper.selectById(id);
            if (activity == null) continue;
            try {
                LambdaUpdateWrapper<Activity> uw = new LambdaUpdateWrapper<>();
                uw.eq(Activity::getId, id).set(Activity::getDeleted, 1);
                activityMapper.update(null, uw);
                evictActivityDetail(id);
                writeOpLog(id, userId, "BATCH_DELETE", "{}");
                operationLogService.log("DELETE", id, activity.getTitle(), null, true, null);
            } catch (Exception e) {
                operationLogService.log("DELETE", id, null, null, false, e.getMessage());
            }
        }
    }

    @Override
    @Transactional
    public void rollback(Long id, Long changeLogId, Integer version) {
        Activity current = mustGet(id);
        assertCanManage(current);

        ActivityChangeLog log = changeLogMapper.selectById(changeLogId);
        if (log == null || !Objects.equals(log.getActivityId(), id)) {
            throw new ApiException(404, HttpStatus.NOT_FOUND, "Change log not found");
        }
        try {
            Activity beforeSnapshot = objectMapper.readValue(log.getBeforeData(), Activity.class);
            Activity patch = new Activity();
            patch.setId(id);
            patch.setVersion(version);
            patch.setTitle(beforeSnapshot.getTitle());
            patch.setSummary(beforeSnapshot.getSummary());
            patch.setCoverUrl(beforeSnapshot.getCoverUrl());
            patch.setContent(beforeSnapshot.getContent());
            patch.setContentType(beforeSnapshot.getContentType());
            patch.setLocation(beforeSnapshot.getLocation());
            patch.setStartTime(beforeSnapshot.getStartTime());
            patch.setEndTime(beforeSnapshot.getEndTime());
            patch.setRegStartTime(beforeSnapshot.getRegStartTime());
            patch.setRegEndTime(beforeSnapshot.getRegEndTime());
            patch.setFormSchema(beforeSnapshot.getFormSchema());
            patch.setChannels(beforeSnapshot.getChannels());
            patch.setWhitelistEnabled(beforeSnapshot.getWhitelistEnabled());
            patch.setStockTotal(beforeSnapshot.getStockTotal());
            patch.setStockAvailable(beforeSnapshot.getStockAvailable());
            patch.setPerUserLimit(beforeSnapshot.getPerUserLimit());
            patch.setCurrentVariant(beforeSnapshot.getCurrentVariant());
            patch.setStatus(beforeSnapshot.getStatus());
            patch.setPublishAt(beforeSnapshot.getPublishAt());
            patch.setOfflineAt(beforeSnapshot.getOfflineAt());
            patch.setAuditReason(beforeSnapshot.getAuditReason());
            patch.setAuditBy(beforeSnapshot.getAuditBy());
            patch.setAuditAt(beforeSnapshot.getAuditAt());
            patch.setUpdatedBy(SecurityUtils.getUserId());
            patch.setUpdatedAt(LocalDateTime.now());
            boolean ok = this.updateById(patch);
            if (!ok) {
                throw new ApiException(409, HttpStatus.CONFLICT, "Version conflict");
            }
            Activity after = mustGet(id);
            writeChangeLog(id, SecurityUtils.getUserId(), "ROLLBACK", current, after);
            evictActivityDetail(id);
        } catch (JsonProcessingException e) {
            throw new ApiException(400, HttpStatus.BAD_REQUEST, "Invalid snapshot");
        }
    }

    @Override
    @Transactional
    public Long upsertVariant(Long activityId, ActivityVariantUpsertRequest req) {
        Activity activity = mustGet(activityId);
        assertCanManage(activity);
        requireUpdatableStatus(activity);

        LambdaQueryWrapper<ActivityVariant> qw = new LambdaQueryWrapper<>();
        qw.eq(ActivityVariant::getActivityId, activityId).eq(ActivityVariant::getVariantCode, req.getVariantCode())
                .orderByDesc(ActivityVariant::getVariantVersion).last("limit 1");
        ActivityVariant latest = activityVariantMapper.selectOne(qw);
        int nextVersion = latest == null ? 1 : latest.getVariantVersion() + 1;

        ActivityVariant v = new ActivityVariant();
        v.setActivityId(activityId);
        v.setVariantCode(req.getVariantCode());
        v.setVariantVersion(nextVersion);
        v.setTitle(req.getTitle());
        v.setSummary(req.getSummary());
        v.setCoverUrl(req.getCoverUrl());
        v.setContent(req.getContent());
        v.setContentType(req.getContentType());
        v.setCreatedBy(SecurityUtils.getUserId());
        v.setCreatedAt(LocalDateTime.now());
        activityVariantMapper.insert(v);
        String diffData;
        try {
            diffData = objectMapper.writeValueAsString(java.util.Map.of(
                    "variantCode", v.getVariantCode(),
                    "variantVersion", v.getVariantVersion(),
                    "variantId", v.getId()
            ));
        } catch (JsonProcessingException e) {
            diffData = "{}";
        }
        writeOpLog(activityId, SecurityUtils.getUserId(), "VARIANT_CREATE", diffData);
        return v.getId();
    }

    @Override
    @Transactional
    public void activateVariant(Long activityId, String variantCode, Integer version) {
        Activity before = mustGet(activityId);
        assertCanManage(before);
        requireUpdatableStatus(before);

        LambdaQueryWrapper<ActivityVariant> qw = new LambdaQueryWrapper<>();
        qw.eq(ActivityVariant::getActivityId, activityId).eq(ActivityVariant::getVariantCode, variantCode)
                .orderByDesc(ActivityVariant::getVariantVersion).last("limit 1");
        ActivityVariant latest = activityVariantMapper.selectOne(qw);
        if (latest == null) {
            throw new ApiException(404, HttpStatus.NOT_FOUND, "Variant not found");
        }

        Activity patch = new Activity();
        patch.setId(activityId);
        patch.setVersion(version);
        patch.setCurrentVariant(variantCode);
        patch.setTitle(latest.getTitle());
        patch.setSummary(latest.getSummary());
        patch.setCoverUrl(latest.getCoverUrl());
        patch.setContent(latest.getContent());
        patch.setContentType(latest.getContentType());
        patch.setUpdatedBy(SecurityUtils.getUserId());
        patch.setUpdatedAt(LocalDateTime.now());
        boolean ok = this.updateById(patch);
        if (!ok) {
            throw new ApiException(409, HttpStatus.CONFLICT, "Version conflict");
        }
        Activity after = mustGet(activityId);
        writeChangeLog(activityId, SecurityUtils.getUserId(), "ACTIVATE_VARIANT", before, after);
        evictActivityDetail(activityId);
    }

    @Override
    public PageResponse<ActivityChangeLog> pageChangeLogs(Long activityId, long page, long size) {
        mustGet(activityId);
        long p = Math.max(1, page);
        long s = Math.min(200, Math.max(1, size));
        Page<ActivityChangeLog> mpPage = new Page<>(p, s);
        LambdaQueryWrapper<ActivityChangeLog> qw = new LambdaQueryWrapper<>();
        qw.eq(ActivityChangeLog::getActivityId, activityId).orderByDesc(ActivityChangeLog::getId);
        Page<ActivityChangeLog> result = changeLogMapper.selectPage(mpPage, qw);
        return PageResponse.of(p, s, result.getTotal(), result.getRecords());
    }

    @Override
    @Transactional
    public void addWhitelist(Long activityId, List<Long> userIds) {
        Activity activity = mustGet(activityId);
        assertCanManage(activity);
        if (userIds == null || userIds.isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        String diffData;
        try {
            diffData = objectMapper.writeValueAsString(java.util.Map.of("addUserIds", userIds));
        } catch (JsonProcessingException e) {
            diffData = "{}";
        }
        for (Long uid : userIds) {
            if (uid == null) {
                continue;
            }
            Long cnt = whitelistMapper.selectCount(new LambdaQueryWrapper<ActivityWhitelist>()
                    .eq(ActivityWhitelist::getActivityId, activityId)
                    .eq(ActivityWhitelist::getUserId, uid));
            if (cnt != null && cnt > 0) {
                continue;
            }
            ActivityWhitelist wl = new ActivityWhitelist();
            wl.setActivityId(activityId);
            wl.setUserId(uid);
            wl.setCreatedAt(now);
            whitelistMapper.insert(wl);
        }
        writeOpLog(activityId, SecurityUtils.getUserId(), "WHITELIST_ADD", diffData);
        operationLogService.log("WHITELIST_ADD", activityId, activity.getTitle(), java.util.Map.of("userIds", userIds), true, null);
    }

    @Override
    @Transactional
    public void removeWhitelist(Long activityId, List<Long> userIds) {
        Activity activity = mustGet(activityId);
        assertCanManage(activity);
        if (userIds == null || userIds.isEmpty()) {
            return;
        }
        whitelistMapper.delete(new LambdaQueryWrapper<ActivityWhitelist>()
                .eq(ActivityWhitelist::getActivityId, activityId)
                .in(ActivityWhitelist::getUserId, userIds));
        String diffData;
        try {
            diffData = objectMapper.writeValueAsString(java.util.Map.of("removeUserIds", userIds));
        } catch (JsonProcessingException e) {
            diffData = "{}";
        }
        writeOpLog(activityId, SecurityUtils.getUserId(), "WHITELIST_REMOVE", diffData);
        operationLogService.log("WHITELIST_REMOVE", activityId, activity.getTitle(), java.util.Map.of("userIds", userIds), true, null);
    }

    @Override
    public List<Long> listWhitelistUserIds(Long activityId) {
        Activity activity = mustGet(activityId);
        assertCanManage(activity);
        List<ActivityWhitelist> list = whitelistMapper.selectList(new LambdaQueryWrapper<ActivityWhitelist>()
                .eq(ActivityWhitelist::getActivityId, activityId)
                .orderByDesc(ActivityWhitelist::getId)
                .last("limit 5000"));
        return list.stream().map(ActivityWhitelist::getUserId).collect(Collectors.toList());
    }

    @Override
    public ActivityReserveResponse reserveForUser(Long activityId) {
        Activity activity = mustGet(activityId);
        if (!STATUS_ONLINE.equals(activity.getStatus())) {
            return ActivityReserveResponse.fail(activityId, "NOT_ONLINE");
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
                return ActivityReserveResponse.fail(activityId, "NOT_IN_WHITELIST");
            }
        }

        initStockToRedisIfAbsent(activity);

        String stockKey = cacheKeyStock(activityId);
        String userKey = cacheKeyUserLimit(activityId, userId);
        int limit = activity.getPerUserLimit() == null ? 0 : activity.getPerUserLimit();

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
        if (result == null) {
            return ActivityReserveResponse.fail(activityId, "ERROR");
        }
        if (result == -1) {
            return ActivityReserveResponse.fail(activityId, "NO_STOCK");
        }
        if (result == -2) {
            return ActivityReserveResponse.fail(activityId, "LIMIT_REACHED");
        }
        return ActivityReserveResponse.ok(activityId, result.intValue());
    }

    @Override
    @Transactional
    public void runScheduleTick() {
        LocalDateTime now = LocalDateTime.now();

        List<Activity> toPublish = activityMapper.selectList(new LambdaQueryWrapper<Activity>()
                .eq(Activity::getStatus, STATUS_APPROVED)
                .isNotNull(Activity::getPublishAt)
                .le(Activity::getPublishAt, now)
                .last("limit 200"));
        for (Activity a : toPublish) {
            Activity before = a;
            Activity patch = new Activity();
            patch.setId(a.getId());
            patch.setVersion(a.getVersion());
            patch.setStatus(STATUS_ONLINE);
            patch.setUpdatedBy(0L);
            patch.setUpdatedAt(now);
            boolean ok = this.updateById(patch);
            if (ok) {
                Activity after = mustGet(a.getId());
                writeChangeLog(after.getId(), 0L, "SCHEDULE_PUBLISH", before, after);
                cacheActivityDetail(after);
                initStockToRedisIfAbsent(after);
            }
        }

        List<Activity> toOffline = activityMapper.selectList(new LambdaQueryWrapper<Activity>()
                .eq(Activity::getStatus, STATUS_ONLINE)
                .isNotNull(Activity::getOfflineAt)
                .le(Activity::getOfflineAt, now)
                .last("limit 200"));
        for (Activity a : toOffline) {
            Activity before = a;
            Activity patch = new Activity();
            patch.setId(a.getId());
            patch.setVersion(a.getVersion());
            patch.setStatus(STATUS_OFFLINE);
            patch.setUpdatedBy(0L);
            patch.setUpdatedAt(now);
            boolean ok = this.updateById(patch);
            if (ok) {
                Activity after = mustGet(a.getId());
                writeChangeLog(after.getId(), 0L, "SCHEDULE_OFFLINE", before, after);
                operationLogService.log("OFFLINE", a.getId(), after.getTitle(), after, true, "Scheduled offline");
                evictActivityDetail(after.getId());
            }
        }
    }
}
