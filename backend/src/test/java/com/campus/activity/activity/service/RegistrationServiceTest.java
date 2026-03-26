package com.campus.activity.activity.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.activity.activity.dto.RegistrationAuditRequest;
import com.campus.activity.activity.dto.RegistrationStatsResponse;
import com.campus.activity.activity.entity.Activity;
import com.campus.activity.activity.entity.Registration;
import com.campus.activity.activity.enums.RegistrationStatus;
import com.campus.activity.activity.mapper.ActivityMapper;
import com.campus.activity.activity.mapper.RegistrationMapper;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class RegistrationServiceTest {

    @Autowired
    private RegistrationService registrationService;

    @Autowired
    private ActivityMapper activityMapper;

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
        when(valueOperations.get(anyString())).thenReturn(null);
        doNothing().when(valueOperations).set(anyString(), anyString());
        when(stringRedisTemplate.execute(any(), any(), any())).thenReturn(1L);
        when(valueOperations.increment(anyString())).thenReturn(1L);
        when(valueOperations.decrement(anyString())).thenReturn(0L);
        when(stringRedisTemplate.delete(anyString())).thenReturn(true);

        Activity a = new Activity();
        a.setTitle("Test Activity");
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
    public void testStudentRegisterSuccess() {
        mockLogin(studentUserId);
        Long regId = registrationService.register(activityId, "{}");
        assertNotNull(regId);

        Registration reg = registrationMapper.selectById(regId);
        assertEquals(RegistrationStatus.PENDING.name(), reg.getStatus());

        Activity a = activityMapper.selectById(activityId);
        assertEquals(99, a.getStockAvailable());
    }

    @Test
    public void testStudentCancelRegistration() {
        mockLogin(studentUserId);
        Long regId = registrationService.register(activityId, "{}");

        registrationService.cancel(regId);

        Registration reg = registrationMapper.selectById(regId);
        assertEquals(RegistrationStatus.CANCELED.name(), reg.getStatus());

        Activity a = activityMapper.selectById(activityId);
        assertEquals(100, a.getStockAvailable());
    }

    @Test
    public void testAdminAuditRegistration() {
        mockLogin(studentUserId);
        Long regId = registrationService.register(activityId, "{}");

        mockLogin(adminUserId); // Admin to audit
        RegistrationAuditRequest req = new RegistrationAuditRequest();
        req.setStatus(RegistrationStatus.APPROVED.name());
        registrationService.audit(regId, req);

        Registration reg = registrationMapper.selectById(regId);
        assertEquals(RegistrationStatus.APPROVED.name(), reg.getStatus());
    }

    @Test
    public void testRegistrationStats() {
        mockLogin(studentUserId);
        registrationService.register(activityId, "{}");

        mockLogin(adminUserId);
        RegistrationStatsResponse stats = registrationService.getStats(activityId);
        assertEquals(1, stats.getTotal());
        assertEquals(1, stats.getPending());
        assertEquals(0, stats.getApproved());
    }
}
