package com.campus.activity.activity.controller;

import com.campus.activity.activity.dto.ActivityCreateRequest;
import com.campus.activity.activity.dto.ActivityUpdateRequest;
import com.campus.activity.activity.dto.ActivityVariantUpsertRequest;
import com.campus.activity.activity.dto.ActivityWorkflowRequest;
import com.campus.activity.activity.service.ActivityService;
import com.campus.activity.common.IdListRequest;
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
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class ActivityAdminControllerTest {

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
        req.setStockTotal(10);
        String resp = mockMvc.perform(post("/api/v1/admin/activities")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(resp).get("data").asLong();
    }

    private JsonNode getAdminDetail(String token, Long id) throws Exception {
        String resp = mockMvc.perform(get("/api/v1/admin/activities/" + id)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(resp).get("data");
    }

    @Test
    public void testCreateActivityAsOwner() throws Exception {
        String ownerToken = TestTokenUtil.login(mockMvc, objectMapper, "owner1", "123456");
        Long id = createDraft(ownerToken, "迎新晚会");
        mockMvc.perform(get("/api/v1/admin/activities/" + id)
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.activity.title").value("迎新晚会"))
                .andExpect(jsonPath("$.data.activity.status").value("DRAFT"));
    }

    @Test
    public void testCreateActivityValidationFail() throws Exception {
        String ownerToken = TestTokenUtil.login(mockMvc, objectMapper, "owner1", "123456");
        ActivityCreateRequest req = new ActivityCreateRequest();
        mockMvc.perform(post("/api/v1/admin/activities")
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    public void testListActivitiesAsOwnerOnlyOwn() throws Exception {
        String ownerToken = TestTokenUtil.login(mockMvc, objectMapper, "owner1", "123456");
        String adminToken = TestTokenUtil.login(mockMvc, objectMapper, "admin2", "123456");
        createDraft(ownerToken, "owner-1");
        createDraft(adminToken, "admin-1");

        mockMvc.perform(get("/api/v1/admin/activities")
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records").isArray())
                .andExpect(jsonPath("$.data.records[0].title").value("owner-1"));
    }

    @Test
    public void testUpdateActivitySuccess() throws Exception {
        String ownerToken = TestTokenUtil.login(mockMvc, objectMapper, "owner1", "123456");
        Long id = createDraft(ownerToken, "t1");
        int version = getAdminDetail(ownerToken, id).get("activity").get("version").asInt();

        ActivityUpdateRequest req = new ActivityUpdateRequest();
        req.setVersion(version);
        req.setTitle("t2");

        mockMvc.perform(put("/api/v1/admin/activities/" + id)
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(get("/api/v1/admin/activities/" + id)
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.activity.title").value("t2"));
    }

    @Test
    public void testUpdateActivityVersionConflict() throws Exception {
        String ownerToken = TestTokenUtil.login(mockMvc, objectMapper, "owner1", "123456");
        Long id = createDraft(ownerToken, "t1");

        ActivityUpdateRequest req1 = new ActivityUpdateRequest();
        req1.setVersion(0);
        req1.setTitle("t2");
        mockMvc.perform(put("/api/v1/admin/activities/" + id)
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isOk());

        ActivityUpdateRequest req2 = new ActivityUpdateRequest();
        req2.setVersion(0);
        req2.setTitle("t3");
        mockMvc.perform(put("/api/v1/admin/activities/" + id)
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409));
    }

    @Test
    public void testSubmitReview() throws Exception {
        String ownerToken = TestTokenUtil.login(mockMvc, objectMapper, "owner1", "123456");
        Long id = createDraft(ownerToken, "t1");
        int version = getAdminDetail(ownerToken, id).get("activity").get("version").asInt();

        ActivityWorkflowRequest req = new ActivityWorkflowRequest();
        req.setVersion(version);
        mockMvc.perform(post("/api/v1/admin/activities/" + id + "/submit-review")
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/admin/activities/" + id)
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.activity.status").value("PENDING_REVIEW"));
    }

    @Test
    public void testWithdraw() throws Exception {
        String ownerToken = TestTokenUtil.login(mockMvc, objectMapper, "owner1", "123456");
        Long id = createDraft(ownerToken, "t1");
        int v0 = getAdminDetail(ownerToken, id).get("activity").get("version").asInt();

        ActivityWorkflowRequest submit = new ActivityWorkflowRequest();
        submit.setVersion(v0);
        mockMvc.perform(post("/api/v1/admin/activities/" + id + "/submit-review")
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submit)))
                .andExpect(status().isOk());

        int v1 = getAdminDetail(ownerToken, id).get("activity").get("version").asInt();
        ActivityWorkflowRequest withdraw = new ActivityWorkflowRequest();
        withdraw.setVersion(v1);
        mockMvc.perform(post("/api/v1/admin/activities/" + id + "/withdraw")
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(withdraw)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/admin/activities/" + id)
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.activity.status").value("DRAFT"));
    }

    @Test
    public void testApproveImmediateOnline() throws Exception {
        String ownerToken = TestTokenUtil.login(mockMvc, objectMapper, "owner1", "123456");
        String counselorToken = TestTokenUtil.login(mockMvc, objectMapper, "counselor1", "123456");
        Long id = createDraft(ownerToken, "t1");

        int v0 = getAdminDetail(ownerToken, id).get("activity").get("version").asInt();
        ActivityWorkflowRequest submit = new ActivityWorkflowRequest();
        submit.setVersion(v0);
        mockMvc.perform(post("/api/v1/admin/activities/" + id + "/submit-review")
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submit)))
                .andExpect(status().isOk());

        int v1 = getAdminDetail(ownerToken, id).get("activity").get("version").asInt();
        ActivityWorkflowRequest approve = new ActivityWorkflowRequest();
        approve.setVersion(v1);
        mockMvc.perform(post("/api/v1/admin/activities/" + id + "/approve")
                        .header("Authorization", bearer(counselorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(approve)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/admin/activities/" + id)
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.activity.status").value("ONLINE"));
    }

    @Test
    public void testApproveScheduledAndScheduleTick() throws Exception {
        String ownerToken = TestTokenUtil.login(mockMvc, objectMapper, "owner1", "123456");
        String counselorToken = TestTokenUtil.login(mockMvc, objectMapper, "counselor1", "123456");
        Long id = createDraft(ownerToken, "t1");

        int v0 = getAdminDetail(ownerToken, id).get("activity").get("version").asInt();
        ActivityWorkflowRequest submit = new ActivityWorkflowRequest();
        submit.setVersion(v0);
        mockMvc.perform(post("/api/v1/admin/activities/" + id + "/submit-review")
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submit)))
                .andExpect(status().isOk());

        int v1 = getAdminDetail(ownerToken, id).get("activity").get("version").asInt();
        ActivityWorkflowRequest approve = new ActivityWorkflowRequest();
        approve.setVersion(v1);
        approve.setPublishAt(LocalDateTime.now().plusSeconds(1));
        mockMvc.perform(post("/api/v1/admin/activities/" + id + "/approve")
                        .header("Authorization", bearer(counselorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(approve)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/admin/activities/" + id)
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.activity.status").value("APPROVED"));

        Thread.sleep(1200);
        activityService.runScheduleTick();

        mockMvc.perform(get("/api/v1/admin/activities/" + id)
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.activity.status").value("ONLINE"));
    }

    @Test
    public void testRevokeSchedule() throws Exception {
        String ownerToken = TestTokenUtil.login(mockMvc, objectMapper, "owner1", "123456");
        String counselorToken = TestTokenUtil.login(mockMvc, objectMapper, "counselor1", "123456");
        Long id = createDraft(ownerToken, "t1");

        int v0 = getAdminDetail(ownerToken, id).get("activity").get("version").asInt();
        ActivityWorkflowRequest submit = new ActivityWorkflowRequest();
        submit.setVersion(v0);
        mockMvc.perform(post("/api/v1/admin/activities/" + id + "/submit-review")
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submit)))
                .andExpect(status().isOk());

        int v1 = getAdminDetail(ownerToken, id).get("activity").get("version").asInt();
        ActivityWorkflowRequest approve = new ActivityWorkflowRequest();
        approve.setVersion(v1);
        approve.setPublishAt(LocalDateTime.now().plusHours(1));
        mockMvc.perform(post("/api/v1/admin/activities/" + id + "/approve")
                        .header("Authorization", bearer(counselorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(approve)))
                .andExpect(status().isOk());

        int v2 = getAdminDetail(ownerToken, id).get("activity").get("version").asInt();
        ActivityWorkflowRequest revoke = new ActivityWorkflowRequest();
        revoke.setVersion(v2);
        mockMvc.perform(post("/api/v1/admin/activities/" + id + "/revoke-schedule")
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(revoke)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/admin/activities/" + id)
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.activity.status").value("DRAFT"));
    }

    @Test
    public void testReject() throws Exception {
        String ownerToken = TestTokenUtil.login(mockMvc, objectMapper, "owner1", "123456");
        String counselorToken = TestTokenUtil.login(mockMvc, objectMapper, "counselor1", "123456");
        Long id = createDraft(ownerToken, "t1");

        int v0 = getAdminDetail(ownerToken, id).get("activity").get("version").asInt();
        ActivityWorkflowRequest submit = new ActivityWorkflowRequest();
        submit.setVersion(v0);
        mockMvc.perform(post("/api/v1/admin/activities/" + id + "/submit-review")
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submit)))
                .andExpect(status().isOk());

        int v1 = getAdminDetail(ownerToken, id).get("activity").get("version").asInt();
        ActivityWorkflowRequest reject = new ActivityWorkflowRequest();
        reject.setVersion(v1);
        reject.setReason("信息不完整");
        mockMvc.perform(post("/api/v1/admin/activities/" + id + "/reject")
                        .header("Authorization", bearer(counselorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reject)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/admin/activities/" + id)
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.activity.status").value("REJECTED"))
                .andExpect(jsonPath("$.data.activity.auditReason").value("信息不完整"));
    }

    @Test
    public void testOffline() throws Exception {
        String ownerToken = TestTokenUtil.login(mockMvc, objectMapper, "owner1", "123456");
        String counselorToken = TestTokenUtil.login(mockMvc, objectMapper, "counselor1", "123456");
        Long id = createDraft(ownerToken, "t1");

        int v0 = getAdminDetail(ownerToken, id).get("activity").get("version").asInt();
        ActivityWorkflowRequest submit = new ActivityWorkflowRequest();
        submit.setVersion(v0);
        mockMvc.perform(post("/api/v1/admin/activities/" + id + "/submit-review")
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submit)))
                .andExpect(status().isOk());

        int v1 = getAdminDetail(ownerToken, id).get("activity").get("version").asInt();
        ActivityWorkflowRequest approve = new ActivityWorkflowRequest();
        approve.setVersion(v1);
        mockMvc.perform(post("/api/v1/admin/activities/" + id + "/approve")
                        .header("Authorization", bearer(counselorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(approve)))
                .andExpect(status().isOk());

        int v2 = getAdminDetail(ownerToken, id).get("activity").get("version").asInt();
        ActivityWorkflowRequest offline = new ActivityWorkflowRequest();
        offline.setVersion(v2);
        mockMvc.perform(post("/api/v1/admin/activities/" + id + "/offline")
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(offline)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/admin/activities/" + id)
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.activity.status").value("OFFLINE"));
    }

    @Test
    public void testCannotEditOnline() throws Exception {
        String ownerToken = TestTokenUtil.login(mockMvc, objectMapper, "owner1", "123456");
        String counselorToken = TestTokenUtil.login(mockMvc, objectMapper, "counselor1", "123456");
        Long id = createDraft(ownerToken, "t1");

        int v0 = getAdminDetail(ownerToken, id).get("activity").get("version").asInt();
        ActivityWorkflowRequest submit = new ActivityWorkflowRequest();
        submit.setVersion(v0);
        mockMvc.perform(post("/api/v1/admin/activities/" + id + "/submit-review")
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submit)))
                .andExpect(status().isOk());

        int v1 = getAdminDetail(ownerToken, id).get("activity").get("version").asInt();
        ActivityWorkflowRequest approve = new ActivityWorkflowRequest();
        approve.setVersion(v1);
        mockMvc.perform(post("/api/v1/admin/activities/" + id + "/approve")
                        .header("Authorization", bearer(counselorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(approve)))
                .andExpect(status().isOk());

        ActivityUpdateRequest update = new ActivityUpdateRequest();
        update.setVersion(getAdminDetail(ownerToken, id).get("activity").get("version").asInt());
        update.setTitle("xx");
        mockMvc.perform(put("/api/v1/admin/activities/" + id)
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    public void testVariantCreateAndActivate() throws Exception {
        String ownerToken = TestTokenUtil.login(mockMvc, objectMapper, "owner1", "123456");
        Long id = createDraft(ownerToken, "t1");
        int v0 = getAdminDetail(ownerToken, id).get("activity").get("version").asInt();

        ActivityVariantUpsertRequest vreq = new ActivityVariantUpsertRequest();
        vreq.setVariantCode("B");
        vreq.setTitle("t1-b");
        mockMvc.perform(post("/api/v1/admin/activities/" + id + "/variants")
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vreq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isNumber());

        mockMvc.perform(post("/api/v1/admin/activities/" + id + "/variants/B/activate")
                        .header("Authorization", bearer(ownerToken))
                        .param("version", String.valueOf(v0))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/admin/activities/" + id)
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.activity.currentVariant").value("B"))
                .andExpect(jsonPath("$.data.activity.title").value("t1-b"));
    }

    @Test
    public void testChangeLogsPagination() throws Exception {
        String ownerToken = TestTokenUtil.login(mockMvc, objectMapper, "owner1", "123456");
        Long id = createDraft(ownerToken, "t1");
        int v0 = getAdminDetail(ownerToken, id).get("activity").get("version").asInt();
        ActivityUpdateRequest req = new ActivityUpdateRequest();
        req.setVersion(v0);
        req.setSummary("s1");
        mockMvc.perform(put("/api/v1/admin/activities/" + id)
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/admin/activities/" + id + "/change-logs")
                        .header("Authorization", bearer(ownerToken))
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records").isArray())
                .andExpect(jsonPath("$.data.records.length()").value(greaterThan(0)));
    }

    @Test
    public void testRollback() throws Exception {
        String ownerToken = TestTokenUtil.login(mockMvc, objectMapper, "owner1", "123456");
        Long id = createDraft(ownerToken, "t1");
        int v0 = getAdminDetail(ownerToken, id).get("activity").get("version").asInt();

        ActivityUpdateRequest req = new ActivityUpdateRequest();
        req.setVersion(v0);
        req.setTitle("t2");
        mockMvc.perform(put("/api/v1/admin/activities/" + id)
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        String logsResp = mockMvc.perform(get("/api/v1/admin/activities/" + id + "/change-logs")
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode logId = objectMapper.readTree(logsResp).get("data").get("records").get(0).get("id");

        int v1 = getAdminDetail(ownerToken, id).get("activity").get("version").asInt();
        mockMvc.perform(post("/api/v1/admin/activities/" + id + "/rollback/" + logId.asLong())
                        .header("Authorization", bearer(ownerToken))
                        .param("version", String.valueOf(v1)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/admin/activities/" + id)
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.activity.title").value("t1"));
    }

    @Test
    public void testWhitelistAddListRemove() throws Exception {
        String ownerToken = TestTokenUtil.login(mockMvc, objectMapper, "owner1", "123456");
        Long id = createDraft(ownerToken, "t1");

        IdListRequest add = new IdListRequest();
        add.setIds(List.of(1L, 2L));
        mockMvc.perform(post("/api/v1/admin/activities/" + id + "/whitelist/add")
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(add)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/admin/activities/" + id + "/whitelist")
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)));

        IdListRequest remove = new IdListRequest();
        remove.setIds(List.of(2L));
        mockMvc.perform(post("/api/v1/admin/activities/" + id + "/whitelist/remove")
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(remove)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/admin/activities/" + id + "/whitelist")
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)));
    }

    @Test
    public void testReserveNotOnline() throws Exception {
        String ownerToken = TestTokenUtil.login(mockMvc, objectMapper, "owner1", "123456");
        Long id = createDraft(ownerToken, "t1");
        mockMvc.perform(post("/api/v1/activities/" + id + "/reserve")
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.result").value("NOT_ONLINE"));
    }

    @Test
    public void testExportCsv() throws Exception {
        String ownerToken = TestTokenUtil.login(mockMvc, objectMapper, "owner1", "123456");
        createDraft(ownerToken, "csv-1");
        mockMvc.perform(get("/api/v1/admin/activities/export")
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("activities.csv")))
                .andExpect(content().string(containsString("csv-1")));
    }

    @Test
    public void testDeleteForbiddenForOwner() throws Exception {
        String ownerToken = TestTokenUtil.login(mockMvc, objectMapper, "owner1", "123456");
        Long id = createDraft(ownerToken, "Forbidden Delete");
        int version = getAdminDetail(ownerToken, id).get("activity").get("version").asInt();

        mockMvc.perform(delete("/api/v1/admin/activities/" + id + "?version=" + version)
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testDeleteAsAdmin2() throws Exception {
        String ownerToken = TestTokenUtil.login(mockMvc, objectMapper, "owner1", "123456");
        String adminToken = TestTokenUtil.login(mockMvc, objectMapper, "admin2", "123456");
        Long id = createDraft(ownerToken, "Admin Delete");
        int version = getAdminDetail(ownerToken, id).get("activity").get("version").asInt();

        mockMvc.perform(delete("/api/v1/admin/activities/" + id + "?version=" + version)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk());
    }

    @Test
    public void testApproveForbiddenForOwner() throws Exception {
        String ownerToken = TestTokenUtil.login(mockMvc, objectMapper, "owner1", "123456");
        Long id = createDraft(ownerToken, "t1");
        int v0 = getAdminDetail(ownerToken, id).get("activity").get("version").asInt();
        ActivityWorkflowRequest submit = new ActivityWorkflowRequest();
        submit.setVersion(v0);
        mockMvc.perform(post("/api/v1/admin/activities/" + id + "/submit-review")
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submit)))
                .andExpect(status().isOk());
        int v1 = getAdminDetail(ownerToken, id).get("activity").get("version").asInt();
        ActivityWorkflowRequest approve = new ActivityWorkflowRequest();
        approve.setVersion(v1);
        mockMvc.perform(post("/api/v1/admin/activities/" + id + "/approve")
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(approve)))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testSearchAndStatusFilter() throws Exception {
        String ownerToken = TestTokenUtil.login(mockMvc, objectMapper, "owner1", "123456");
        createDraft(ownerToken, "hello-1");
        createDraft(ownerToken, "world-1");
        mockMvc.perform(get("/api/v1/admin/activities")
                        .header("Authorization", bearer(ownerToken))
                        .param("keyword", "hello"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].title").value("hello-1"));
        mockMvc.perform(get("/api/v1/admin/activities")
                        .header("Authorization", bearer(ownerToken))
                        .param("status", "DRAFT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records.length()").value(greaterThan(0)));
    }

    @Test
    public void testPagination() throws Exception {
        String ownerToken = TestTokenUtil.login(mockMvc, objectMapper, "owner1", "123456");
        createDraft(ownerToken, "p1");
        createDraft(ownerToken, "p2");
        mockMvc.perform(get("/api/v1/admin/activities")
                        .header("Authorization", bearer(ownerToken))
                        .param("page", "1")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records.length()").value(1));
    }

    @Test
    public void testWithdrawInvalidStatus() throws Exception {
        String ownerToken = TestTokenUtil.login(mockMvc, objectMapper, "owner1", "123456");
        Long id = createDraft(ownerToken, "t1");
        ActivityWorkflowRequest withdraw = new ActivityWorkflowRequest();
        withdraw.setVersion(0);
        mockMvc.perform(post("/api/v1/admin/activities/" + id + "/withdraw")
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(withdraw)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    public void testApproveInvalidStatus() throws Exception {
        String counselorToken = TestTokenUtil.login(mockMvc, objectMapper, "counselor1", "123456");
        String ownerToken = TestTokenUtil.login(mockMvc, objectMapper, "owner1", "123456");
        Long id = createDraft(ownerToken, "t1");
        ActivityWorkflowRequest approve = new ActivityWorkflowRequest();
        approve.setVersion(0);
        mockMvc.perform(post("/api/v1/admin/activities/" + id + "/approve")
                        .header("Authorization", bearer(counselorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(approve)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    public void testRejectInvalidStatus() throws Exception {
        String counselorToken = TestTokenUtil.login(mockMvc, objectMapper, "counselor1", "123456");
        String ownerToken = TestTokenUtil.login(mockMvc, objectMapper, "owner1", "123456");
        Long id = createDraft(ownerToken, "t1");
        ActivityWorkflowRequest reject = new ActivityWorkflowRequest();
        reject.setVersion(0);
        reject.setReason("x");
        mockMvc.perform(post("/api/v1/admin/activities/" + id + "/reject")
                        .header("Authorization", bearer(counselorToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reject)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    public void testOfflineInvalidStatus() throws Exception {
        String ownerToken = TestTokenUtil.login(mockMvc, objectMapper, "owner1", "123456");
        Long id = createDraft(ownerToken, "t1");
        ActivityWorkflowRequest offline = new ActivityWorkflowRequest();
        offline.setVersion(0);
        mockMvc.perform(post("/api/v1/admin/activities/" + id + "/offline")
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(offline)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    public void testRevokeScheduleInvalidWhenNotScheduled() throws Exception {
        String ownerToken = TestTokenUtil.login(mockMvc, objectMapper, "owner1", "123456");
        Long id = createDraft(ownerToken, "t1");
        ActivityWorkflowRequest revoke = new ActivityWorkflowRequest();
        revoke.setVersion(0);
        mockMvc.perform(post("/api/v1/admin/activities/" + id + "/revoke-schedule")
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(revoke)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    public void testSubmitTwiceInvalid() throws Exception {
        String ownerToken = TestTokenUtil.login(mockMvc, objectMapper, "owner1", "123456");
        Long id = createDraft(ownerToken, "t1");
        ActivityWorkflowRequest submit = new ActivityWorkflowRequest();
        submit.setVersion(0);
        mockMvc.perform(post("/api/v1/admin/activities/" + id + "/submit-review")
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submit)))
                .andExpect(status().isOk());
        int v1 = getAdminDetail(ownerToken, id).get("activity").get("version").asInt();
        ActivityWorkflowRequest submit2 = new ActivityWorkflowRequest();
        submit2.setVersion(v1);
        mockMvc.perform(post("/api/v1/admin/activities/" + id + "/submit-review")
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submit2)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testWhitelistValidationEmpty() throws Exception {
        String ownerToken = TestTokenUtil.login(mockMvc, objectMapper, "owner1", "123456");
        Long id = createDraft(ownerToken, "t1");
        IdListRequest req = new IdListRequest();
        req.setIds(List.of());
        mockMvc.perform(post("/api/v1/admin/activities/" + id + "/whitelist/add")
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    public void testExportCsvFilterKeyword() throws Exception {
        String ownerToken = TestTokenUtil.login(mockMvc, objectMapper, "owner1", "123456");
        createDraft(ownerToken, "csv-hello");
        createDraft(ownerToken, "csv-world");
        mockMvc.perform(get("/api/v1/admin/activities/export")
                        .header("Authorization", bearer(ownerToken))
                        .param("keyword", "hello"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("csv-hello")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("csv-world"))));
    }

    @Test
    public void testRollbackInvalidLogId() throws Exception {
        String ownerToken = TestTokenUtil.login(mockMvc, objectMapper, "owner1", "123456");
        Long id = createDraft(ownerToken, "Rollback Invalid");
        int version = getAdminDetail(ownerToken, id).get("activity").get("version").asInt();

        mockMvc.perform(post("/api/v1/admin/activities/" + id + "/rollback/99999?version=" + version)
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testCreateActivityWithNewFields() throws Exception {
        String ownerToken = TestTokenUtil.login(mockMvc, objectMapper, "owner1", "123456");
        ActivityCreateRequest req = new ActivityCreateRequest();
        req.setTitle("New Fields Test");
        req.setLocation("Campus Hall");
        req.setStartTime(LocalDateTime.now().plusDays(1));
        req.setEndTime(LocalDateTime.now().plusDays(1).plusHours(2));
        req.setRegStartTime(LocalDateTime.now());
        req.setRegEndTime(LocalDateTime.now().plusHours(12));

        String resp = mockMvc.perform(post("/api/v1/admin/activities")
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Long id = objectMapper.readTree(resp).get("data").asLong();

        mockMvc.perform(get("/api/v1/admin/activities/" + id)
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.activity.title").value("New Fields Test"))
                .andExpect(jsonPath("$.data.activity.location").value("Campus Hall"))
                .andExpect(jsonPath("$.data.activity.startTime").exists())
                .andExpect(jsonPath("$.data.activity.regStartTime").exists());
    }

    @Test
    public void testConcurrentUpdate() throws Exception {
        String ownerToken = TestTokenUtil.login(mockMvc, objectMapper, "owner1", "123456");
        
        // 1. Create
        ActivityCreateRequest createReq = new ActivityCreateRequest();
        createReq.setTitle("Concurrent Test");
        String createResp = mockMvc.perform(post("/api/v1/admin/activities")
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Long id = objectMapper.readTree(createResp).get("data").asLong();

        // 2. Get initial version
        String detailResp = mockMvc.perform(get("/api/v1/admin/activities/" + id)
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        int version = objectMapper.readTree(detailResp).get("data").get("activity").get("version").asInt();

        // 3. First update (Success)
        ActivityUpdateRequest updateReq1 = new ActivityUpdateRequest();
        updateReq1.setTitle("Updated 1");
        updateReq1.setVersion(version);
        mockMvc.perform(put("/api/v1/admin/activities/" + id)
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq1)))
                .andExpect(status().isOk());

        // 4. Second update with same version (Conflict)
        ActivityUpdateRequest updateReq2 = new ActivityUpdateRequest();
        updateReq2.setTitle("Updated 2");
        updateReq2.setVersion(version);
        mockMvc.perform(put("/api/v1/admin/activities/" + id)
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq2)))
                .andExpect(status().isConflict());
    }

    @Test
    public void testBatchOperations() throws Exception {
        String adminToken = TestTokenUtil.login(mockMvc, objectMapper, "admin2", "123456");
        
        // Create two activities
        ActivityCreateRequest req = new ActivityCreateRequest();
        req.setTitle("Batch 1");
        String r1 = mockMvc.perform(post("/api/v1/admin/activities").header("Authorization", bearer(adminToken))
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req))).andReturn().getResponse().getContentAsString();
        Long id1 = objectMapper.readTree(r1).get("data").asLong();
        
        req.setTitle("Batch 2");
        String r2 = mockMvc.perform(post("/api/v1/admin/activities").header("Authorization", bearer(adminToken))
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req))).andReturn().getResponse().getContentAsString();
        Long id2 = objectMapper.readTree(r2).get("data").asLong();

        // Batch delete
        IdListRequest batchReq = new IdListRequest();
        batchReq.setIds(List.of(id1, id2));
        mockMvc.perform(post("/api/v1/admin/activities/batch-delete")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(batchReq)))
                .andExpect(status().isOk());

        // Verify deleted
        mockMvc.perform(get("/api/v1/admin/activities/" + id1).header("Authorization", bearer(adminToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testUnauthorizedAccess() throws Exception {
        mockMvc.perform(get("/api/v1/admin/activities"))
                .andExpect(status().isForbidden());
    }
}
