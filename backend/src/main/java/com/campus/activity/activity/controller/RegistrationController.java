package com.campus.activity.activity.controller;

import com.campus.activity.activity.dto.RegistrationCreateRequest;
import com.campus.activity.activity.entity.Registration;
import com.campus.activity.activity.service.RegistrationService;
import com.campus.activity.common.PageResponse;
import com.campus.activity.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/registrations")
@RequiredArgsConstructor
@Validated
public class RegistrationController {

    private final RegistrationService registrationService;

    @PostMapping
    public Result<Long> register(@RequestBody RegistrationCreateRequest req) {
        Long id = registrationService.register(req.getActivityId(), req.getExtraData());
        return Result.success(id);
    }

    @PostMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id) {
        registrationService.cancel(id);
        return Result.success(null);
    }

    @GetMapping("/my")
    public Result<PageResponse<Registration>> getMyRegistrations(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String status) {
        return Result.success(registrationService.getMyRegistrations(page, size, status));
    }

    @GetMapping("/{id}")
    public Result<Registration> getDetail(@PathVariable Long id) {
        return Result.success(registrationService.getDetail(id));
    }
}
