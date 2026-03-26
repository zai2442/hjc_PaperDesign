package com.campus.activity.activity.controller;

import com.campus.activity.activity.dto.RegistrationAuditRequest;
import com.campus.activity.activity.dto.RegistrationStatsResponse;
import com.campus.activity.activity.entity.Registration;
import com.campus.activity.activity.service.RegistrationService;
import com.campus.activity.common.PageResponse;
import com.campus.activity.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/registrations")
@RequiredArgsConstructor
@Validated
public class RegistrationAdminController {

    private final RegistrationService registrationService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','COUNSELOR','CLUB_OWNER')")
    public Result<PageResponse<Registration>> pageAdminRegistrations(
            @RequestParam Long activityId,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        return Result.success(registrationService.getAdminRegistrations(activityId, status, keyword, page, size));
    }

    @PostMapping("/{id}/audit")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','COUNSELOR','CLUB_OWNER')")
    public Result<Void> audit(@PathVariable Long id, @RequestBody RegistrationAuditRequest req) {
        registrationService.audit(id, req);
        return Result.success(null);
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','COUNSELOR','CLUB_OWNER')")
    public Result<RegistrationStatsResponse> getStats(@RequestParam Long activityId) {
        return Result.success(registrationService.getStats(activityId));
    }
}
