package com.campus.activity.user.service;

import com.campus.activity.user.dto.RegisterDto;
import com.campus.activity.user.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class UserServiceTest {

    @Autowired
    private UserService userService;

    @Test
    public void testRegisterAndFind() {
        RegisterDto dto = new RegisterDto();
        dto.setUsername("testuser");
        dto.setPassword("password123");
        dto.setEmail("test@example.com");
        dto.setPhone("13800138000");

        userService.register(dto);

        User savedUser = userService.lambdaQuery().eq(User::getUsername, "testuser").one();
        assertNotNull(savedUser);
        assertEquals("test@example.com", savedUser.getEmail());
    }
}
