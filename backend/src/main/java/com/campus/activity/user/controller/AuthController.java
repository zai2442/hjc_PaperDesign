package com.campus.activity.user.controller;

import com.campus.activity.common.Result;
import com.campus.activity.user.dto.LoginDto;
import com.campus.activity.user.dto.RegisterDto;
import com.campus.activity.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/login")
    public Result<String> login(@Validated @RequestBody LoginDto loginDto) {
        try {
            String token = userService.login(loginDto);
            return Result.success(token);
        } catch (org.springframework.security.core.AuthenticationException e) {
            e.printStackTrace();
            return Result.error(401, "Auth failed: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(500, "System error: " + e.getMessage());
        }
    }

    @PostMapping("/register")
    public Result<Void> register(@Validated @RequestBody RegisterDto registerDto) {
        try {
            userService.register(registerDto);
            return Result.success(null);
        } catch (Exception e) {
            return Result.error(400, e.getMessage());
        }
    }
}
