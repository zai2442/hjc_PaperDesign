package com.campus.activity.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.activity.security.JwtUtils;
import com.campus.activity.security.LoginUser;
import com.campus.activity.user.dto.LoginDto;
import com.campus.activity.user.dto.RegisterDto;
import com.campus.activity.user.entity.User;
import com.campus.activity.user.mapper.UserMapper;
import com.campus.activity.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.campus.activity.user.dto.UserInfoDto;
import com.campus.activity.user.entity.Role;
import com.campus.activity.user.mapper.RoleMapper;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final PasswordEncoder passwordEncoder;
    private final RoleMapper roleMapper;

    @Override
    public String login(LoginDto loginDto) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginDto.getUsername(), loginDto.getPassword()));
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        return jwtUtils.generateToken(loginUser);
    }

    @Override
    @Transactional
    public void register(RegisterDto registerDto) {
        if (count(new QueryWrapper<User>().eq("username", registerDto.getUsername())) > 0) {
            throw new RuntimeException("Username already exists");
        }
        User user = new User();
        user.setUsername(registerDto.getUsername());
        user.setPassword(passwordEncoder.encode(registerDto.getPassword()));
        user.setEmail(registerDto.getEmail());
        user.setPhone(registerDto.getPhone());
        user.setStatus(1);
        save(user);
    }

    @Override
    public void updateUserInfo(User user) {
        updateById(user);
    }

    @Override
    public void resetPassword(Long userId, String oldPassword, String newPassword) {
        User user = getById(userId);
        if (user == null) {
            throw new RuntimeException("User not found");
        }
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new RuntimeException("Incorrect old password");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        updateById(user);
    }

    @Override
    public List<UserInfoDto> getAllUsers() {
        List<User> users = list();
        return users.stream().map(user -> {
            List<Role> roles = roleMapper.selectRolesByUserId(user.getId());
            return UserInfoDto.from(user, roles);
        }).collect(Collectors.toList());
    }
}
