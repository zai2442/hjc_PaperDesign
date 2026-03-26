package com.campus.activity.activity.controller;

import com.campus.activity.activity.entity.Activity;
import com.campus.activity.activity.mapper.ActivityMapper;
import com.campus.activity.testutil.TestTokenUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class RegistrationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ActivityMapper activityMapper;

    @MockBean
    private StringRedisTemplate stringRedisTemplate;

    private ValueOperations<String, String> valueOperations;

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
        a.setTitle("Controller Test Activity");
        a.setStatus("ONLINE");
        a.setStockTotal(100);
        a.setStockAvailable(100);
        a.setPerUserLimit(1);
        a.setCreatedBy(1L);
        a.setUpdatedBy(1L);
        a.setCreatedAt(LocalDateTime.now());
        a.setUpdatedAt(LocalDateTime.now());
        a.setVersion(0);
        a.setDeleted(0);
        activityMapper.insert(a);
        activityId = a.getId();
    }

    @Test
    public void testStudentRegisterAndQueryMyRegistrations() throws Exception {
        String token = TestTokenUtil.login(mockMvc, objectMapper, "student1", "123456");

        String resp = mockMvc.perform(post("/api/v1/registrations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("activityId", activityId, "extraData", "{}"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(resp);
        long regId = json.get("data").asLong();

        mockMvc.perform(get("/api/v1/registrations/my")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(greaterThanOrEqualTo(1)));

        mockMvc.perform(get("/api/v1/registrations/" + regId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(regId));
    }

    @Test
    public void testAdminAuditAndStats() throws Exception {
        String studentToken = TestTokenUtil.login(mockMvc, objectMapper, "student1", "123456");
        String adminToken = TestTokenUtil.login(mockMvc, objectMapper, "admin", "123456");

        String createResp = mockMvc.perform(post("/api/v1/registrations")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("activityId", activityId))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long regId = objectMapper.readTree(createResp).get("data").asLong();

        mockMvc.perform(post("/api/v1/admin/registrations/" + regId + "/audit")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "APPROVED", "reason", "ok"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(get("/api/v1/admin/registrations")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("activityId", String.valueOf(activityId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(greaterThanOrEqualTo(1)));

        mockMvc.perform(get("/api/v1/admin/registrations/stats")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("activityId", String.valueOf(activityId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.approved").value(1));
    }
}

