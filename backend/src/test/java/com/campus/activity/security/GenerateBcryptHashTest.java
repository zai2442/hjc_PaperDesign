package com.campus.activity.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class GenerateBcryptHashTest {

    @Test
    public void printHashFor123456() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hash = encoder.encode("123456");
        System.out.println("BCrypt(123456)=" + hash);
    }
}

