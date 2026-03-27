package com.campus.activity.log.controller;

import com.campus.activity.common.PageResponse;
import com.campus.activity.common.Result;
import com.campus.activity.log.dto.LogQueryRequest;
import com.campus.activity.log.entity.OperationLog;
import com.campus.activity.log.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/logs")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ROLE_SUPER_ADMIN', 'ROLE_COUNSELOR')")
public class LogAdminController {

    private final OperationLogService operationLogService;

    @GetMapping
    public Result<PageResponse<OperationLog>> queryLogs(LogQueryRequest request) {
        return Result.success(operationLogService.queryLogs(request));
    }
}
