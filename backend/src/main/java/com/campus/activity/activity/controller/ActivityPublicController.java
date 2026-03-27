package com.campus.activity.activity.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.activity.activity.dto.ActivityReserveResponse;
import com.campus.activity.activity.dto.RegistrationCreateRequest;
import com.campus.activity.activity.entity.Activity;
import com.campus.activity.activity.mapper.ActivityMapper;
import com.campus.activity.activity.service.ActivityService;
import com.campus.activity.common.PageResponse;
import com.campus.activity.common.Result;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping
    public Result<PageResponse<Activity>> page(@RequestParam(defaultValue = "1") long page,
                                              @RequestParam(defaultValue = "10") long size,
                                              @RequestParam(required = false) String keyword,
                                              @RequestParam(required = false) String type,
                                              @RequestParam(required = false) String startTime,
                                              @RequestParam(required = false) String endTime,
                                              @RequestParam(defaultValue = "false") boolean hasSpots,
                                              @RequestParam(required = false) String statusCategory,
                                              @RequestParam(required = false) String sortBy) {
        log.info("Public activity query: page={}, size={}, keyword={}, type={}, startTime={}, endTime={}, hasSpots={}, statusCategory={}, sortBy={}",
                page, size, keyword, type, startTime, endTime, hasSpots, statusCategory, sortBy);
        long p = Math.max(1, page);
        long s = Math.min(50, Math.max(1, size));
        LocalDateTime now = LocalDateTime.now();

        LambdaQueryWrapper<Activity> qw = new LambdaQueryWrapper<>();
        qw.eq(Activity::getStatus, STATUS_ONLINE)
                .le(Activity::getPublishAt, now)
                .and(w -> w.isNull(Activity::getOfflineAt).or().gt(Activity::getOfflineAt, now));
                
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
            // 报名中: activity not yet started (start_time is in the future or NULL)
            qw.and(w -> w.isNull(Activity::getStartTime).or().gt(Activity::getStartTime, now));
        } else if ("IN_PROGRESS".equals(statusCategory)) {
            // 进行中: now is between start_time and end_time
            qw.isNotNull(Activity::getStartTime)
              .le(Activity::getStartTime, now)
              .and(w -> w.isNull(Activity::getEndTime).or().ge(Activity::getEndTime, now));
        } else if ("ENDED".equals(statusCategory)) {
            // 已结束: end_time has passed
            qw.isNotNull(Activity::getEndTime)
              .lt(Activity::getEndTime, now);
        }
        
        if ("HOTTEST".equals(sortBy)) {
            qw.last("ORDER BY (stock_total - stock_available) DESC, id DESC");
        } else if ("UPCOMING".equals(sortBy)) {
            qw.orderByAsc(Activity::getStartTime).orderByDesc(Activity::getId);
        } else {
            // LATEST or default
            qw.orderByDesc(Activity::getPublishAt).orderByDesc(Activity::getId);
        }

        Page<Activity> mpPage = new Page<>(p, s);
        Page<Activity> result = activityMapper.selectPage(mpPage, qw);
        return Result.success(PageResponse.of(p, s, result.getTotal(), result.getRecords()));
    }

    @GetMapping("/{id}")
    public Result<Activity> detail(@PathVariable Long id) {
        Activity a = activityMapper.selectById(id);
        if (a == null) {
            return Result.error(404, "Activity not found");
        }
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
        return Result.success(a);
    }

    @PostMapping("/{id}/reserve")
    public Result<ActivityReserveResponse> reserve(@PathVariable Long id) {
        return Result.success(activityService.reserveForUser(id));
    }
}
