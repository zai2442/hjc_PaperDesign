package com.campus.activity.activity.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.campus.activity.activity.dto.RegistrationAuditRequest;
import com.campus.activity.activity.dto.RegistrationStatsResponse;
import com.campus.activity.activity.entity.Registration;
import com.campus.activity.common.PageResponse;

public interface RegistrationService extends IService<Registration> {
    
    // 学生端
    Long register(Long activityId, String extraData);
    void cancel(Long id);
    PageResponse<Registration> getMyRegistrations(long page, long size, String status);
    Registration getDetail(Long id);
    
    // 管理端
    PageResponse<Registration> getAdminRegistrations(Long activityId, String status, String keyword, long page, long size);
    void audit(Long id, RegistrationAuditRequest request);
    RegistrationStatsResponse getStats(Long activityId);
}
