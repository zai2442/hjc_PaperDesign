package com.campus.activity.activity.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.activity.activity.dto.ActivityReserveResponse;
import com.campus.activity.activity.entity.Activity;
import com.campus.activity.activity.mapper.ActivityMapper;
import com.campus.activity.activity.service.ActivityService;
import com.campus.activity.common.PageResponse;
import com.campus.activity.common.Result;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.campus.activity.activity.dto.ActivityPublicResponse;
import com.campus.activity.activity.entity.Registration;
import com.campus.activity.activity.entity.Tag;
import com.campus.activity.activity.mapper.RegistrationMapper;
import com.campus.activity.activity.mapper.TagMapper;
import com.campus.activity.security.SecurityUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/activities")
@RequiredArgsConstructor
@Validated
public class ActivityPublicController {

    private static final Logger log = LoggerFactory.getLogger(ActivityPublicController.class);
    private static final String STATUS_ONLINE = "ONLINE";

    private final ActivityMapper activityMapper;
    private final ActivityService activityService;
    private final RegistrationMapper registrationMapper;
    private final TagMapper tagMapper;

    @GetMapping
    public Result<PageResponse<ActivityPublicResponse>> page(@RequestParam(defaultValue = "1") long page,
                                              @RequestParam(defaultValue = "10") long size,
                                              @RequestParam(required = false) String keyword,
                                              @RequestParam(required = false) String type,
                                              @RequestParam(required = false) String startTime,
                                              @RequestParam(required = false) String endTime,
                                              @RequestParam(defaultValue = "false") boolean hasSpots,
                                              @RequestParam(required = false) String statusCategory,
                                              @RequestParam(required = false) String registrationStatus,
                                              @RequestParam(required = false) String sortBy) {
        log.info("Public activity query: page={}, size={}, keyword={}, type={}, startTime={}, endTime={}, hasSpots={}, statusCategory={}, registrationStatus={}, sortBy={}",
                page, size, keyword, type, startTime, endTime, hasSpots, statusCategory, registrationStatus, sortBy);
        long p = Math.max(1, page);
        long s = Math.min(50, Math.max(1, size));
        LocalDateTime now = LocalDateTime.now();

        Long currentUserId = SecurityUtils.getUserId();
        
        LambdaQueryWrapper<Activity> qw = new LambdaQueryWrapper<>();
        // 公开列表逻辑：要求已发布且在发布期内；或者是活动创建者本人（用于预览稿件）
        qw.and(w -> {
            w.and(w2 -> w2.eq(Activity::getStatus, STATUS_ONLINE)
                    .le(Activity::getPublishAt, now)
                    .and(w3 -> w3.isNull(Activity::getOfflineAt).or().gt(Activity::getOfflineAt, now)));
            if (currentUserId != null) {
                w.or(w4 -> w4.eq(Activity::getCreatedBy, currentUserId));
            }
        });
        
        if (keyword != null && !keyword.isBlank()) {
            qw.like(Activity::getTitle, keyword.trim());
        }
        if (type != null && !type.isBlank()) {
            qw.eq(Activity::getContentType, type.trim());
        }
        if (startTime != null && !startTime.isBlank()) {
            qw.ge(Activity::getStartTime, LocalDateTime.parse(startTime));
        }
        if (endTime != null && !endTime.isBlank()) {
            qw.le(Activity::getEndTime, LocalDateTime.parse(endTime));
        }
        if (hasSpots) {
            qw.gt(Activity::getStockAvailable, 0);
        }

        if ("ENROLLING".equals(statusCategory)) {
            // 报名中: 
            // 如果用户已登录，按要求仅显示该用户待审核(PENDING)的活动
            // 如果用户未登录，则显示处于报名期内的活动
            if (currentUserId != null) {
                qw.inSql(Activity::getId, "SELECT activity_id FROM act_registration WHERE user_id = " + currentUserId + " AND status = 'PENDING'");
            } else {
                qw.and(w -> w.isNull(Activity::getRegEndTime).or().gt(Activity::getRegEndTime, now))
                  .and(w -> w.isNull(Activity::getEndTime).or().gt(Activity::getEndTime, now));
                qw.and(w -> w.isNull(Activity::getRegStartTime).or().le(Activity::getRegStartTime, now));
            }
        } else if ("IN_PROGRESS".equals(statusCategory)) {
            // 进行中: 活动已经开始且尚未结束
            qw.isNotNull(Activity::getStartTime)
              .le(Activity::getStartTime, now)
              .and(w -> w.isNull(Activity::getEndTime).or().ge(Activity::getEndTime, now));
        } else if ("ENDED".equals(statusCategory)) {
            // 已结束: 活动结束时间已过
            qw.isNotNull(Activity::getEndTime)
              .lt(Activity::getEndTime, now);
        }

        if (registrationStatus != null && !registrationStatus.isBlank() && currentUserId != null) {
            qw.inSql(Activity::getId, "SELECT activity_id FROM act_registration WHERE user_id = " + currentUserId + " AND status = '" + registrationStatus.trim() + "'");
        }
        
        if ("HOTTEST".equals(sortBy)) {
            qw.last("ORDER BY (stock_total - stock_available) DESC, id DESC");
        } else if ("UPCOMING".equals(sortBy)) {
            qw.orderByAsc(Activity::getStartTime).orderByDesc(Activity::getId);
        } else if ("CATEGORY".equals(sortBy)) {
            qw.orderByAsc(Activity::getContentType).orderByDesc(Activity::getId);
        } else {
            // LATEST or default
            qw.orderByDesc(Activity::getPublishAt).orderByDesc(Activity::getId);
        }

        Page<Activity> mpPage = new Page<>(p, s);
        Page<Activity> result = activityMapper.selectPage(mpPage, qw);
        
        List<ActivityPublicResponse> records = result.getRecords().stream().map(a -> {
            String regStatus = null;
            Long regId = null;
            if (currentUserId != null) {
                Registration reg = registrationMapper.selectOne(new LambdaQueryWrapper<Registration>()
                        .eq(Registration::getActivityId, a.getId())
                        .eq(Registration::getUserId, currentUserId)
                        .orderByDesc(Registration::getId)
                        .last("limit 1"));
                if (reg != null) {
                    regStatus = reg.getStatus();
                    regId = reg.getId();
                }
            }
            List<Tag> tags = tagMapper.findTagsByActivityId(a.getId());
            return ActivityPublicResponse.from(a, regStatus, regId, tags);
        }).collect(Collectors.toList());

        return Result.success(PageResponse.of(p, s, result.getTotal(), records));
    }

    @GetMapping("/{id}")
    public Result<ActivityPublicResponse> detail(@PathVariable Long id) {
        Activity a = activityMapper.selectById(id);
        if (a == null) {
            return Result.error(404, "Activity not found");
        }
        
        Long userId = SecurityUtils.getUserId();
        boolean isCreator = userId != null && userId.equals(a.getCreatedBy());
        
        if (!isCreator) {
            if (!STATUS_ONLINE.equals(a.getStatus())) {
                return Result.error(404, "Activity not found");
            }
            LocalDateTime now = LocalDateTime.now();
            if (a.getPublishAt() != null && a.getPublishAt().isAfter(now)) {
                return Result.error(404, "Activity not found");
            }
            if (a.getOfflineAt() != null && !a.getOfflineAt().isAfter(now)) {
                return Result.error(404, "Activity not found");
            }
        }
        String regStatus = null;
        Long regId = null;
        if (userId != null) {
            Registration reg = registrationMapper.selectOne(new LambdaQueryWrapper<Registration>()
                    .eq(Registration::getActivityId, id)
                    .eq(Registration::getUserId, userId)
                    .orderByDesc(Registration::getId)
                    .last("limit 1"));
            if (reg != null) {
                regStatus = reg.getStatus();
                regId = reg.getId();
            }
        }
        List<Tag> tags = tagMapper.findTagsByActivityId(id);
        return Result.success(ActivityPublicResponse.from(a, regStatus, regId, tags));
    }

    @PostMapping("/{id}/reserve")
    public Result<ActivityReserveResponse> reserve(@PathVariable Long id) {
        return Result.success(activityService.reserveForUser(id));
    }
}
