package com.campus.activity.log.service;

import com.campus.activity.common.PageResponse;
import com.campus.activity.log.dto.LogQueryRequest;
import com.campus.activity.log.entity.OperationLog;
import com.campus.activity.log.websocket.LogWebSocketHandler;
import com.campus.activity.security.LoginUser;
import com.campus.activity.security.SecurityUtils;
import com.campus.activity.user.entity.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@Transactional
public class OperationLogServiceTest {

    @Autowired
    private OperationLogService operationLogService;

    @MockBean
    private LogWebSocketHandler logWebSocketHandler;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        // Clear data before each test if necessary
    }

    @Test
    void testLogAndQuery() {
        // 1. Log an operation
        OperationLog log = new OperationLog();
        log.setOperatorId(1L);
        log.setOperatorUsername("admin");
        log.setOperatorNickname("Admin User");
        log.setActivityId(100L);
        log.setActivityTitle("Test Activity");
        log.setOpType("OFFLINE");
        log.setOpDetail("{\"test\": true}");
        log.setOpResult(1);
        log.setCreatedAt(LocalDateTime.now());

        operationLogService.log(log);

        // Verify WebSocket notification was called
        verify(logWebSocketHandler, times(1)).notifyNewLog();

        // 2. Query the log
        LogQueryRequest request = new LogQueryRequest();
        request.setKeyword("admin");
        PageResponse<OperationLog> response = operationLogService.queryLogs(request);

        assertNotNull(response);
        assertTrue(response.getTotal() >= 1);
        OperationLog found = response.getRecords().get(0);
        assertEquals("admin", found.getOperatorUsername());
        assertEquals("OFFLINE", found.getOpType());
    }

    @Test
    void testLogWithFilters() {
        // Log multiple entries
        logEntry("admin", "OFFLINE", 100L, "Activity A");
        logEntry("user1", "WHITELIST_ADD", 101L, "Activity B");
        logEntry("admin", "DELETE", 102L, "Activity C");

        // Filter by opType
        LogQueryRequest req1 = new LogQueryRequest();
        req1.setOpTypes(Collections.singletonList("OFFLINE"));
        PageResponse<OperationLog> res1 = operationLogService.queryLogs(req1);
        assertEquals(1, res1.getRecords().stream().filter(l -> "OFFLINE".equals(l.getOpType())).count());

        // Filter by keyword (Activity title)
        LogQueryRequest req2 = new LogQueryRequest();
        req2.setKeyword("Activity B");
        PageResponse<OperationLog> res2 = operationLogService.queryLogs(req2);
        assertEquals(1, res2.getRecords().size());
        assertEquals("Activity B", res2.getRecords().get(0).getActivityTitle());
    }

    @Test
    void testLogHelper() {
        User user = new User();
        user.setId(1L);
        user.setUsername("admin");
        user.setNickname("Admin");
        LoginUser loginUser = new LoginUser(user, Collections.emptyList(), Collections.emptyList());

        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(SecurityUtils::getLoginUser).thenReturn(loginUser);

            operationLogService.log("DELETE", 200L, "Del Activity", Collections.singletonMap("id", 200), true, null);

            LogQueryRequest req = new LogQueryRequest();
            req.setKeyword("Del Activity");
            PageResponse<OperationLog> res = operationLogService.queryLogs(req);
            assertEquals(1, res.getRecords().size());
            assertEquals("DELETE", res.getRecords().get(0).getOpType());
        }
    }

    private void logEntry(String username, String opType, Long activityId, String title) {
        OperationLog log = new OperationLog();
        log.setOperatorId(1L);
        log.setOperatorUsername(username);
        log.setOpType(opType);
        log.setActivityId(activityId);
        log.setActivityTitle(title);
        log.setOpResult(1);
        log.setCreatedAt(LocalDateTime.now());
        operationLogService.log(log);
    }
}
