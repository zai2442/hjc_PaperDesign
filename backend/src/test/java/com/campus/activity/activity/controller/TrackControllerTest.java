package com.campus.activity.activity.controller;

import com.campus.activity.testutil.TestTokenUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class TrackControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testTrackWithoutAuthAllowed() throws Exception {
        mockMvc.perform(post("/api/v1/track")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eventName\":\"page_view\",\"activityId\":1,\"eventData\":\"{}\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    public void testTrackValidationFail() throws Exception {
        mockMvc.perform(post("/api/v1/track")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"activityId\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    public void testTrackWithAuth() throws Exception {
        String token = TestTokenUtil.login(mockMvc, objectMapper, "student1", "123456");
        mockMvc.perform(post("/api/v1/track")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eventName\":\"click\",\"activityId\":1,\"eventData\":\"{\\\"x\\\":1}\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}

