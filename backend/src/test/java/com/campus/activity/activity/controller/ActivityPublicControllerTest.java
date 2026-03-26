package com.campus.activity.activity.controller;

import com.campus.activity.activity.dto.ActivityCreateRequest;
import com.campus.activity.activity.dto.ActivityWorkflowRequest;
import com.campus.activity.activity.service.ActivityService;
import com.campus.activity.testutil.TestTokenUtil;
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

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class ActivityPublicControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ActivityService activityService;

    @MockBean
    private StringRedisTemplate stringRedisTemplate;

    private ValueOperations<String, String> valueOperations;

    @BeforeEach
    public void setupRedisMock() {
        valueOperations = mock(ValueOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        doNothing().when(valueOperations).set(anyString(), anyString());
        when(stringRedisTemplate.delete(anyString())).thenReturn(true);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private Long createDraft(String token, String title) throws Exception {
        ActivityCreateRequest req = new ActivityCreateRequest();
        req.setTitle(title);
        req.setStockTotal(1);
        String resp = mockMvc.perform(post("/api/v1/admin/activities")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(resp).get("data").asLong();
    }

    private int getVersion(String token, Long id) throws Exception {
        String resp = mockMvc.perform(get("/api/v1/admin/activities/" + id)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(resp).get("data").get("version").asInt();
    }

    private void submitAndApproveOnline(Long id, String ownerToken, String counselorToken) throws Exception {
        int v0 = getVersion(ownerToken, id);
        ActivityWorkflowRequest submit = new ActivityWorkflowRequest();
        submit.setVersion(v0);
        mockMvc.perform(post("/api/v1/admin/activities/" + id + "/submit-review")
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submit)))
                .andExpect(status().isOk());

        int v1 = getVersion(ownerToken, id);
        ActivityWorkflowRequest approve = new ActivityWorkflowRequest();
        approve.setVersion(v1);
        mockMvc.perform(post("/api/v1/admin/activities/" + id + "/approve")
                        .header("Authorization", bearer(counselorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(approve)))
                .andExpect(status().isOk());
    }

    @Test
    public void testPublicListEmptyInitially() throws Exception {
        mockMvc.perform(get("/api/v1/activities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray());
    }

    @Test
    public void testPublicDetailNotFoundWhenDraft() throws Exception {
        String ownerToken = TestTokenUtil.login(mockMvc, objectMapper, "owner1", "123456");
        Long id = createDraft(ownerToken, "t1");
        mockMvc.perform(get("/api/v1/activities/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    public void testPublicListAndDetailAfterOnline() throws Exception {
        String ownerToken = TestTokenUtil.login(mockMvc, objectMapper, "owner1", "123456");
        String counselorToken = TestTokenUtil.login(mockMvc, objectMapper, "counselor1", "123456");
        Long id = createDraft(ownerToken, "t-public");
        submitAndApproveOnline(id, ownerToken, counselorToken);

        mockMvc.perform(get("/api/v1/activities").param("keyword", "public"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].title").value("t-public"));

        mockMvc.perform(get("/api/v1/activities/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.title").value("t-public"));
    }

    @Test
    public void testPublicListNotIncludeFuturePublish() throws Exception {
        String ownerToken = TestTokenUtil.login(mockMvc, objectMapper, "owner1", "123456");
        String counselorToken = TestTokenUtil.login(mockMvc, objectMapper, "counselor1", "123456");
        Long id = createDraft(ownerToken, "t-future");

        int v0 = getVersion(ownerToken, id);
        ActivityWorkflowRequest submit = new ActivityWorkflowRequest();
        submit.setVersion(v0);
        mockMvc.perform(post("/api/v1/admin/activities/" + id + "/submit-review")
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submit)))
                .andExpect(status().isOk());

        int v1 = getVersion(ownerToken, id);
        ActivityWorkflowRequest approve = new ActivityWorkflowRequest();
        approve.setVersion(v1);
        approve.setPublishAt(LocalDateTime.now().plusHours(1));
        mockMvc.perform(post("/api/v1/admin/activities/" + id + "/approve")
                        .header("Authorization", bearer(counselorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(approve)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/activities").param("keyword", "future"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records.length()").value(0));
    }

    @Test
    public void testAutoOfflineBySchedule() throws Exception {
        String ownerToken = TestTokenUtil.login(mockMvc, objectMapper, "owner1", "123456");
        String counselorToken = TestTokenUtil.login(mockMvc, objectMapper, "counselor1", "123456");
        Long id = createDraft(ownerToken, "t-offline");
        int v0 = getVersion(ownerToken, id);
        ActivityWorkflowRequest submit = new ActivityWorkflowRequest();
        submit.setVersion(v0);
        mockMvc.perform(post("/api/v1/admin/activities/" + id + "/submit-review")
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submit)))
                .andExpect(status().isOk());

        int v1 = getVersion(ownerToken, id);
        ActivityWorkflowRequest approve = new ActivityWorkflowRequest();
        approve.setVersion(v1);
        approve.setOfflineAt(LocalDateTime.now().plusSeconds(1));
        mockMvc.perform(post("/api/v1/admin/activities/" + id + "/approve")
                        .header("Authorization", bearer(counselorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(approve)))
                .andExpect(status().isOk());

        Thread.sleep(1200);
        activityService.runScheduleTick();

        mockMvc.perform(get("/api/v1/activities/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    public void testRegisterAndAdminSeeRegistrations() throws Exception {
        String ownerToken = TestTokenUtil.login(mockMvc, objectMapper, "owner1", "123456");
        String counselorToken = TestTokenUtil.login(mockMvc, objectMapper, "counselor1", "123456");
        String studentToken = TestTokenUtil.login(mockMvc, objectMapper, "student1", "123456");
        Long id = createDraft(ownerToken, "t-reg");

        submitAndApproveOnline(id, ownerToken, counselorToken);

        mockMvc.perform(post("/api/v1/activities/" + id + "/register")
                        .header("Authorization", bearer(studentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"extraData\":\"{\\\"q1\\\":\\\"a1\\\"}\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(get("/api/v1/admin/activities/" + id + "/registrations")
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records.length()").value(1));

        mockMvc.perform(get("/api/v1/admin/activities/" + id)
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stockAvailable").value(0));
    }

    @Test
    public void testRegisterNoStock() throws Exception {
        String ownerToken = TestTokenUtil.login(mockMvc, objectMapper, "owner1", "123456");
        String counselorToken = TestTokenUtil.login(mockMvc, objectMapper, "counselor1", "123456");
        String student1Token = TestTokenUtil.login(mockMvc, objectMapper, "student1", "123456");
        String student2Token = TestTokenUtil.login(mockMvc, objectMapper, "student2", "123456");
        Long id = createDraft(ownerToken, "t-nostock");
        submitAndApproveOnline(id, ownerToken, counselorToken);

        mockMvc.perform(post("/api/v1/activities/" + id + "/register")
                        .header("Authorization", bearer(student1Token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"extraData\":\"{}\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(post("/api/v1/activities/" + id + "/register")
                        .header("Authorization", bearer(student2Token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"extraData\":\"{}\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    public void testRegisterWhitelistEnforced() throws Exception {
        String ownerToken = TestTokenUtil.login(mockMvc, objectMapper, "owner1", "123456");
        String counselorToken = TestTokenUtil.login(mockMvc, objectMapper, "counselor1", "123456");
        String student1Token = TestTokenUtil.login(mockMvc, objectMapper, "student1", "123456");
        String student2Token = TestTokenUtil.login(mockMvc, objectMapper, "student2", "123456");

        ActivityCreateRequest req = new ActivityCreateRequest();
        req.setTitle("t-wl");
        req.setStockTotal(1);
        req.setWhitelistEnabled(1);
        String createResp = mockMvc.perform(post("/api/v1/admin/activities")
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Long id = objectMapper.readTree(createResp).get("data").asLong();

        mockMvc.perform(post("/api/v1/admin/activities/" + id + "/whitelist/add")
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ids\":[5]}"))
                .andExpect(status().isOk());

        submitAndApproveOnline(id, ownerToken, counselorToken);

        mockMvc.perform(post("/api/v1/activities/" + id + "/register")
                        .header("Authorization", bearer(student2Token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"extraData\":\"{}\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/activities/" + id + "/register")
                        .header("Authorization", bearer(student1Token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"extraData\":\"{}\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
