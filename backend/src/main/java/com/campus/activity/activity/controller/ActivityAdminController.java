package com.campus.activity.activity.controller;

import com.campus.activity.activity.dto.ActivityAdminResponse;
import com.campus.activity.activity.dto.ActivityCreateRequest;
import com.campus.activity.activity.dto.ActivityQueryRequest;
import com.campus.activity.activity.dto.ActivityUpdateRequest;
import com.campus.activity.activity.dto.ActivityVariantUpsertRequest;
import com.campus.activity.activity.dto.ActivityWorkflowRequest;
import com.campus.activity.activity.entity.Activity;
import com.campus.activity.activity.entity.ActivityChangeLog;
import com.campus.activity.activity.entity.Registration;
import com.campus.activity.activity.service.ActivityService;
import com.campus.activity.common.IdListRequest;
import com.campus.activity.common.PageResponse;
import com.campus.activity.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/v1/admin/activities")
@RequiredArgsConstructor
@Validated
public class ActivityAdminController {

    private final ActivityService activityService;

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','COUNSELOR','CLUB_OWNER')")
    public Result<Long> create(@Valid @RequestBody ActivityCreateRequest req) {
        return Result.success(activityService.create(req));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','COUNSELOR','CLUB_OWNER')")
    public Result<PageResponse<Activity>> page(ActivityQueryRequest req) {
        return Result.success(activityService.page(req));
    }

    @GetMapping("/export")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','COUNSELOR','CLUB_OWNER')")
    public ResponseEntity<byte[]> export(ActivityQueryRequest req) {
        byte[] bytes = activityService.exportCsv(req);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv; charset=utf-8"))
                .header("Content-Disposition", "attachment; filename=activities.csv")
                .body(bytes);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','COUNSELOR','CLUB_OWNER')")
    public Result<ActivityAdminResponse> detail(@PathVariable Long id) {
        return Result.success(activityService.getDetailForAdmin(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','COUNSELOR','CLUB_OWNER')")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody ActivityUpdateRequest req) {
        activityService.update(id, req);
        return Result.success(null);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public Result<Void> delete(@PathVariable Long id, @RequestParam Integer version) {
        activityService.delete(id, version);
        return Result.success(null);
    }

    @PostMapping("/{id}/submit-review")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','COUNSELOR','CLUB_OWNER')")
    public Result<Void> submitReview(@PathVariable Long id, @Valid @RequestBody ActivityWorkflowRequest req) {
        activityService.submitReview(id, req);
        return Result.success(null);
    }

    @PostMapping("/{id}/withdraw")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','COUNSELOR','CLUB_OWNER')")
    public Result<Void> withdraw(@PathVariable Long id, @Valid @RequestBody ActivityWorkflowRequest req) {
        activityService.withdraw(id, req);
        return Result.success(null);
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','COUNSELOR')")
    public Result<Void> approve(@PathVariable Long id, @Valid @RequestBody ActivityWorkflowRequest req) {
        activityService.approve(id, req);
        return Result.success(null);
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','COUNSELOR')")
    public Result<Void> reject(@PathVariable Long id, @Valid @RequestBody ActivityWorkflowRequest req) {
        activityService.reject(id, req);
        return Result.success(null);
    }

    @PostMapping("/{id}/revoke-schedule")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','COUNSELOR','CLUB_OWNER')")
    public Result<Void> revokeSchedule(@PathVariable Long id, @Valid @RequestBody ActivityWorkflowRequest req) {
        activityService.revokeSchedule(id, req);
        return Result.success(null);
    }

    @PostMapping("/{id}/offline")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','COUNSELOR','CLUB_OWNER')")
    public Result<Void> offline(@PathVariable Long id, @Valid @RequestBody ActivityWorkflowRequest req) {
        activityService.offline(id, req);
        return Result.success(null);
    }

    @PostMapping("/batch-offline")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','COUNSELOR','CLUB_OWNER')")
    public Result<Void> batchOffline(@Valid @RequestBody IdListRequest req) {
        activityService.batchOffline(req.getIds());
        return Result.success(null);
    }

    @PostMapping("/batch-delete")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public Result<Void> batchDelete(@Valid @RequestBody IdListRequest req) {
        activityService.batchDelete(req.getIds());
        return Result.success(null);
    }
}
