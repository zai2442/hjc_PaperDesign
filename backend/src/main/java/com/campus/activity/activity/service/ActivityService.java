package com.campus.activity.activity.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.campus.activity.activity.dto.ActivityAdminResponse;
import com.campus.activity.activity.dto.ActivityCreateRequest;
import com.campus.activity.activity.dto.ActivityQueryRequest;
import com.campus.activity.activity.dto.ActivityReserveResponse;
import com.campus.activity.activity.dto.ActivityUpdateRequest;
import com.campus.activity.activity.dto.ActivityVariantUpsertRequest;
import com.campus.activity.activity.dto.ActivityWorkflowRequest;
import com.campus.activity.activity.entity.Activity;
import com.campus.activity.activity.entity.ActivityChangeLog;
import com.campus.activity.common.PageResponse;

public interface ActivityService extends IService<Activity> {

    Long create(ActivityCreateRequest req);

    PageResponse<Activity> page(ActivityQueryRequest req);

    byte[] exportCsv(ActivityQueryRequest req);

    ActivityAdminResponse getDetailForAdmin(Long id);

    void update(Long id, ActivityUpdateRequest req);

    void delete(Long id, Integer version);

    void submitReview(Long id, ActivityWorkflowRequest req);

    void withdraw(Long id, ActivityWorkflowRequest req);

    void approve(Long id, ActivityWorkflowRequest req);

    void reject(Long id, ActivityWorkflowRequest req);

    void revokeSchedule(Long id, ActivityWorkflowRequest req);

    void offline(Long id, ActivityWorkflowRequest req);

    void batchOffline(java.util.List<Long> ids);

    void batchDelete(java.util.List<Long> ids);

    void rollback(Long id, Long changeLogId, Integer version);

    Long upsertVariant(Long activityId, ActivityVariantUpsertRequest req);

    void activateVariant(Long activityId, String variantCode, Integer version);

    PageResponse<ActivityChangeLog> pageChangeLogs(Long activityId, long page, long size);

    void addWhitelist(Long activityId, java.util.List<Long> userIds);

    void removeWhitelist(Long activityId, java.util.List<Long> userIds);

    java.util.List<Long> listWhitelistUserIds(Long activityId);

    ActivityReserveResponse reserveForUser(Long activityId);
    void runScheduleTick();
}
