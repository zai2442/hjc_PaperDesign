package com.campus.activity.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class AdminSeedPasswordTest {

    @Test
    public void adminPasswordHashShouldMatch123456() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hashInSchemaSql = "$2a$10$3G3sxx6GinPGsjMOPU76IeywVMptBT0eunqp9rHRPkBGQp0IpafgS";
        assertTrue(encoder.matches("123456", hashInSchemaSql));
    }
}
