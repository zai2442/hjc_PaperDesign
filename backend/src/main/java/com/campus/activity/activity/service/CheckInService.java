package com.campus.activity.activity.service;

import com.campus.activity.activity.dto.CheckInQrcodeResponse;
import com.campus.activity.activity.dto.CheckInScanRequest;
import com.campus.activity.activity.dto.CheckInStatsResponse;

public interface CheckInService {
    void startCheckIn(Long activityId);
    void stopCheckIn(Long activityId);
    CheckInQrcodeResponse getQrcode(Long activityId);
    void scanQrcode(Long activityId, CheckInScanRequest request);
    void manualCheckIn(Long activityId, Long userId);
    CheckInStatsResponse getStats(Long activityId);
}
