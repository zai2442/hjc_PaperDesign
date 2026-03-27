package com.campus.activity.log.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.campus.activity.common.PageResponse;
import com.campus.activity.log.dto.LogQueryRequest;
import com.campus.activity.log.entity.OperationLog;

public interface OperationLogService extends IService<OperationLog> {
    PageResponse<OperationLog> queryLogs(LogQueryRequest request);
    void log(OperationLog log);
    void log(String opType, Long activityId, String activityTitle, Object detail, boolean success, String errorMsg);
}
