package com.campus.activity.activity.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.activity.activity.dto.CheckInManualRequest;
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
import com.campus.activity.common.ApiException;
import com.campus.activity.security.LoginUser;
import com.campus.activity.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class CheckInServiceTest {

    @Autowired
    private CheckInService checkInService;

    @Autowired
    private ActivityMapper activityMapper;

    @Autowired
    private CheckInRecordMapper checkInRecordMapper;

    @Autowired
    private RegistrationMapper registrationMapper;

    @MockBean
    private StringRedisTemplate stringRedisTemplate;

    private ValueOperations<String, String> valueOperations;

    private Long studentUserId = 5L;
    private Long adminUserId = 1L;
    private Long activityId;

    @BeforeEach
    public void setup() {
        valueOperations = mock(ValueOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        doNothing().when(valueOperations).set(anyString(), anyString());
        when(stringRedisTemplate.delete(anyString())).thenReturn(true);

        Activity a = new Activity();
        a.setTitle("CheckIn Test Activity");
        a.setStatus("ONLINE");
        a.setStockTotal(100);
        a.setStockAvailable(100);
        a.setPerUserLimit(1);
        a.setCreatedBy(adminUserId);
        a.setUpdatedBy(adminUserId);
        a.setCreatedAt(LocalDateTime.now());
        a.setUpdatedAt(LocalDateTime.now());
        a.setVersion(0);
        a.setDeleted(0);
        activityMapper.insert(a);
        activityId = a.getId();

        Registration r = new Registration();
        r.setActivityId(activityId);
        r.setUserId(studentUserId);
        r.setStatus(RegistrationStatus.APPROVED.name());
        registrationMapper.insert(r);
    }

    private void mockLogin(Long userId) {
        User u = new User();
        u.setId(userId);
        u.setUsername("user" + userId);
        LoginUser loginUser = new LoginUser(u, Collections.emptyList(), Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities())
        );
    }

    @Test
    public void testStartAndStopCheckIn() {
        mockLogin(adminUserId);
        checkInService.startCheckIn(activityId);
        verify(valueOperations, times(1)).set("activity:checkin:status:" + activityId, "START");

        checkInService.stopCheckIn(activityId);
        verify(stringRedisTemplate, times(1)).delete("activity:checkin:status:" + activityId);
        verify(stringRedisTemplate, times(1)).delete("activity:checkin:token:" + activityId);
    }

    @Test
    public void testGetQrcode() {
        mockLogin(adminUserId);
        when(valueOperations.get("activity:checkin:status:" + activityId)).thenReturn("START");

        CheckInQrcodeResponse response = checkInService.getQrcode(activityId);
        assertNotNull(response.getToken());
        assertTrue(response.getExpireAt() > System.currentTimeMillis());
        verify(valueOperations, times(1)).set(eq("activity:checkin:token:" + activityId), eq(response.getToken()), eq(30L), any());
    }

    @Test
    public void testScanQrcodeSuccess() {
        mockLogin(adminUserId);
        when(valueOperations.get("activity:checkin:status:" + activityId)).thenReturn("START");
        String validToken = "testToken123";
        when(valueOperations.get("activity:checkin:token:" + activityId)).thenReturn(validToken);

        mockLogin(studentUserId);
        CheckInScanRequest req = new CheckInScanRequest();
        req.setToken(validToken);
        checkInService.scanQrcode(activityId, req);

        List<CheckInRecord> records = checkInRecordMapper.selectList(new LambdaQueryWrapper<CheckInRecord>()
                .eq(CheckInRecord::getActivityId, activityId)
                .eq(CheckInRecord::getUserId, studentUserId));
        assertEquals(1, records.size());
        assertEquals("SCAN", records.get(0).getType());

        // Test duplicate scan throws error
        assertThrows(ApiException.class, () -> {
            checkInService.scanQrcode(activityId, req);
        });
    }

    @Test
    public void testScanQrcodeInvalidToken() {
        mockLogin(studentUserId);
        when(valueOperations.get("activity:checkin:status:" + activityId)).thenReturn("START");
        when(valueOperations.get("activity:checkin:token:" + activityId)).thenReturn("validToken");

        CheckInScanRequest req = new CheckInScanRequest();
        req.setToken("invalidToken");
        
        Exception e = assertThrows(ApiException.class, () -> checkInService.scanQrcode(activityId, req));
        assertTrue(e.getMessage().contains("expired or invalid"));
    }

    @Test
    public void testManualCheckIn() {
        mockLogin(adminUserId);
        checkInService.manualCheckIn(activityId, studentUserId);

        List<CheckInRecord> records = checkInRecordMapper.selectList(new LambdaQueryWrapper<CheckInRecord>()
                .eq(CheckInRecord::getActivityId, activityId)
                .eq(CheckInRecord::getUserId, studentUserId));
        assertEquals(1, records.size());
        assertEquals("MANUAL", records.get(0).getType());
    }

    @Test
    public void testGetStats() {
        mockLogin(adminUserId);
        checkInService.manualCheckIn(activityId, studentUserId);

        CheckInStatsResponse stats = checkInService.getStats(activityId);
        assertEquals(1, stats.getTotalRegistered());
        assertEquals(1, stats.getCheckedIn());
        assertEquals(1, stats.getRecords().size());
        assertEquals("MANUAL", stats.getRecords().get(0).getType());
    }
}
