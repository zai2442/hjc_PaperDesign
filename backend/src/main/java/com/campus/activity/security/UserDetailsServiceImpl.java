package com.campus.activity.security;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.campus.activity.user.entity.Permission;
import com.campus.activity.user.entity.Role;
import com.campus.activity.user.entity.User;
import com.campus.activity.user.mapper.PermissionMapper;
import com.campus.activity.user.mapper.RoleMapper;
import com.campus.activity.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userMapper.selectOne(new QueryWrapper<User>().eq("username", username));
        if (user == null) {
            throw new UsernameNotFoundException("User not found");
        }

        System.out.println("Loaded user from DB: " + user.getUsername() + ", hash=" + user.getPassword());

        List<Role> roles = roleMapper.selectRolesByUserId(user.getId());
        List<Permission> permissions = permissionMapper.selectPermissionsByUserId(user.getId());

        return new LoginUser(user, roles, permissions);
    }
}
