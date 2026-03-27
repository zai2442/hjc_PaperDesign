package com.campus.activity.activity.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.activity.activity.dto.CheckInQrcodeResponse;
import com.campus.activity.activity.dto.CheckInScanRequest;
import com.campus.activity.activity.dto.CheckInStatsResponse;
import com.campus.activity.activity.entity.Activity;
import com.campus.activity.activity.entity.CheckInRecord;
import com.campus.activity.activity.entity.Registration;
import com.campus.activity.activity.enums.RegistrationStatus;
import com.campus.activity.activity.mapper.ActivityMapper;
import com.campus.activity.activity.mapper.CheckInRecordMapper;
import com.campus.activity.activity.mapper.RegistrationMapper;
import com.campus.activity.activity.service.CheckInService;
import com.campus.activity.common.ApiException;
import com.campus.activity.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CheckInServiceImpl implements CheckInService {

    private final CheckInRecordMapper checkInRecordMapper;
    private final ActivityMapper activityMapper;
    private final RegistrationMapper registrationMapper;
    private final StringRedisTemplate stringRedisTemplate;

    private static final String ROLE_SUPER_ADMIN = "ROLE_SUPER_ADMIN";
    private static final String ROLE_COUNSELOR = "ROLE_COUNSELOR";

    private boolean canManageAny() {
        return SecurityUtils.hasAuthority(ROLE_SUPER_ADMIN)
                || SecurityUtils.hasAuthority(ROLE_COUNSELOR);
    }

    private void assertCanManage(Activity activity) {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            throw new ApiException(401, HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        if (canManageAny()) {
            return;
        }
        if (!activity.getCreatedBy().equals(userId)) {
            throw new ApiException(403, HttpStatus.FORBIDDEN, "Forbidden");
        }
    }

    @Override
    public void startCheckIn(Long activityId) {
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new ApiException(404, HttpStatus.NOT_FOUND, "Activity not found");
        }
        assertCanManage(activity);
        stringRedisTemplate.opsForValue().set("activity:checkin:status:" + activityId, "START");
    }

    @Override
    public void stopCheckIn(Long activityId) {
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new ApiException(404, HttpStatus.NOT_FOUND, "Activity not found");
        }
        assertCanManage(activity);
        stringRedisTemplate.delete("activity:checkin:status:" + activityId);
        stringRedisTemplate.delete("activity:checkin:token:" + activityId);
    }

    @Override
    public CheckInQrcodeResponse getQrcode(Long activityId) {
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new ApiException(404, HttpStatus.NOT_FOUND, "Activity not found");
        }
        assertCanManage(activity);

        String status = stringRedisTemplate.opsForValue().get("activity:checkin:status:" + activityId);
        if (!"START".equals(status)) {
            return null;
        }

        String token = UUID.randomUUID().toString().replace("-", "");
        stringRedisTemplate.opsForValue().set("activity:checkin:token:" + activityId, token, 30, TimeUnit.SECONDS);

        CheckInQrcodeResponse response = new CheckInQrcodeResponse();
        response.setToken(token);
        response.setExpireAt(System.currentTimeMillis() + 30000);
        return response;
    }

    @Override
    @Transactional
    public void scanQrcode(Long activityId, CheckInScanRequest request) {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            throw new ApiException(401, HttpStatus.UNAUTHORIZED, "Unauthorized");
        }

        String status = stringRedisTemplate.opsForValue().get("activity:checkin:status:" + activityId);
        if (!"START".equals(status)) {
            throw new ApiException(400, HttpStatus.BAD_REQUEST, "Check-in not started");
        }

        String validToken = stringRedisTemplate.opsForValue().get("activity:checkin:token:" + activityId);
        if (validToken == null || !validToken.equals(request.getToken())) {
            throw new ApiException(400, HttpStatus.BAD_REQUEST, "QR code expired or invalid");
        }

        Registration reg = registrationMapper.selectOne(new LambdaQueryWrapper<Registration>()
                .eq(Registration::getActivityId, activityId)
                .eq(Registration::getUserId, userId)
                .last("LIMIT 1"));
        if (reg == null || !RegistrationStatus.APPROVED.name().equals(reg.getStatus())) {
            throw new ApiException(400, HttpStatus.BAD_REQUEST, "Not registered or not approved");
        }

        insertCheckInRecord(activityId, userId, "SCAN");
    }

    @Override
    @Transactional
    public void manualCheckIn(Long activityId, Long userId) {
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new ApiException(404, HttpStatus.NOT_FOUND, "Activity not found");
        }
        assertCanManage(activity);

        Registration reg = registrationMapper.selectOne(new LambdaQueryWrapper<Registration>()
                .eq(Registration::getActivityId, activityId)
                .eq(Registration::getUserId, userId)
                .last("LIMIT 1"));
        if (reg == null || !RegistrationStatus.APPROVED.name().equals(reg.getStatus())) {
            throw new ApiException(400, HttpStatus.BAD_REQUEST, "User is not registered or approved");
        }

        insertCheckInRecord(activityId, userId, "MANUAL");
    }

    private void insertCheckInRecord(Long activityId, Long userId, String type) {
        try {
            CheckInRecord record = new CheckInRecord();
            record.setActivityId(activityId);
            record.setUserId(userId);
            record.setCheckInTime(LocalDateTime.now());
            record.setType(type);
            record.setCreatedAt(LocalDateTime.now());
            record.setUpdatedAt(LocalDateTime.now());
            checkInRecordMapper.insert(record);
        } catch (DuplicateKeyException e) {
            throw new ApiException(400, HttpStatus.BAD_REQUEST, "Already checked in");
        }
    }

    @Override
    public CheckInStatsResponse getStats(Long activityId) {
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new ApiException(404, HttpStatus.NOT_FOUND, "Activity not found");
        }
        assertCanManage(activity);

        Long totalRegistered = registrationMapper.selectCount(new LambdaQueryWrapper<Registration>()
                .eq(Registration::getActivityId, activityId)
                .eq(Registration::getStatus, RegistrationStatus.APPROVED.name()));

        List<CheckInRecord> records = checkInRecordMapper.selectList(new LambdaQueryWrapper<CheckInRecord>()
                .eq(CheckInRecord::getActivityId, activityId)
                .orderByDesc(CheckInRecord::getCheckInTime));

        CheckInStatsResponse resp = new CheckInStatsResponse();
        resp.setTotalRegistered(totalRegistered == null ? 0 : totalRegistered);
        resp.setCheckedIn(records.size());

        List<CheckInStatsResponse.RecordDto> rdTOs = records.stream().map(r -> {
            CheckInStatsResponse.RecordDto dto = new CheckInStatsResponse.RecordDto();
            dto.setUserId(r.getUserId());
            dto.setUsername("User " + r.getUserId()); // Simplified logic
            dto.setCheckInTime(r.getCheckInTime());
            dto.setType(r.getType());
            dto.setCheckedIn(true);
            return dto;
        }).collect(Collectors.toList());

        resp.setRecords(rdTOs);
        return resp;
    }
}
