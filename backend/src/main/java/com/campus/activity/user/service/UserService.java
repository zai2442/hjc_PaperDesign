package com.campus.activity.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.campus.activity.user.dto.LoginDto;
import com.campus.activity.user.dto.RegisterDto;
import com.campus.activity.user.entity.User;

import java.util.List;
import com.campus.activity.user.dto.UserInfoDto;

public interface UserService extends IService<User> {
    String login(LoginDto loginDto);
    void register(RegisterDto registerDto);
    void updateUserInfo(User user);
    void resetPassword(Long userId, String oldPassword, String newPassword);
    List<UserInfoDto> getAllUsers();
}
