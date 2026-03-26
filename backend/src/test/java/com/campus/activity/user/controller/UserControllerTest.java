package com.campus.activity.user.controller;

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

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testSuperAdminCanListUsers() throws Exception {
        // user 1 is super_admin (id=1) in schema.sql
        String token = TestTokenUtil.login(mockMvc, objectMapper, "admin", "123456");

        mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.data[?(@.username=='admin')]").exists());
    }

    @Test
    public void testStudentCannotListUsers() throws Exception {
        // user 5 is student1 (id=5) in schema.sql
        String token = TestTokenUtil.login(mockMvc, objectMapper, "student1", "123456");

        mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.message").value("权限不足"));
    }

    @Test
    public void testDeletedAdminRoleNoLongerAppears() throws Exception {
        // user 1 is super_admin
        String token = TestTokenUtil.login(mockMvc, objectMapper, "admin", "123456");

        mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].roles[*].roleCode", not(hasItem("ROLE_ADMIN"))));
    }

    @Test
    public void testStudentCanAccessOwnInfo() throws Exception {
        String token = TestTokenUtil.login(mockMvc, objectMapper, "student1", "123456");

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("student1"));
    }
}
