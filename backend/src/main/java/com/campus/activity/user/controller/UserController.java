package com.campus.activity.user.controller;

import com.campus.activity.common.Result;
import com.campus.activity.security.LoginUser;
import com.campus.activity.user.dto.PasswordResetDto;
import com.campus.activity.user.dto.UserInfoDto;
import com.campus.activity.user.entity.User;
import com.campus.activity.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import org.springframework.security.access.prepost.PreAuthorize;
import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public Result<List<UserInfoDto>> listUsers() {
        return Result.success(userService.getAllUsers());
    }

    @GetMapping("/me")
    public Result<UserInfoDto> getCurrentUser(@AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(UserInfoDto.from(loginUser.getUser(), loginUser.getRoles()));
    }

    @PutMapping("/me")
    public Result<Void> updateUserInfo(@AuthenticationPrincipal LoginUser loginUser, @RequestBody User user) {
        user.setId(loginUser.getUser().getId());
        user.setPassword(null); // don't update password here
        user.setUsername(null); // don't update username
        userService.updateUserInfo(user);
        return Result.success(null);
    }

    @PostMapping("/me/password")
    public Result<Void> resetPassword(@AuthenticationPrincipal LoginUser loginUser, @Validated @RequestBody PasswordResetDto dto) {
        try {
            userService.resetPassword(loginUser.getUser().getId(), dto.getOldPassword(), dto.getNewPassword());
            return Result.success(null);
        } catch (Exception e) {
            return Result.error(400, e.getMessage());
        }
    }
}
