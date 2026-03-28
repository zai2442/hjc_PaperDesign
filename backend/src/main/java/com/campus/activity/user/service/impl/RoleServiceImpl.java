package com.campus.activity.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.activity.user.entity.Role;
import com.campus.activity.user.entity.RolePermission;
import com.campus.activity.user.entity.UserRole;
import com.campus.activity.user.mapper.RoleMapper;
import com.campus.activity.user.mapper.RolePermissionMapper;
import com.campus.activity.user.mapper.UserRoleMapper;
import com.campus.activity.user.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl extends ServiceImpl<RoleMapper, Role> implements RoleService {

    private static final long ADMIN_USER_ID = 1L;

    private final UserRoleMapper userRoleMapper;
    private final RolePermissionMapper rolePermissionMapper;

    @Override
    @Transactional
    public void assignRolesToUser(Long userId, List<Long> roleIds) {
        if (ADMIN_USER_ID == userId) {
            return; // System admin roles are fixed
        }
        if (roleIds == null || roleIds.isEmpty()) {
            return;
        }
        for (Long roleId : roleIds) {
            if (userRoleMapper.selectCount(new QueryWrapper<UserRole>().eq("user_id", userId).eq("role_id", roleId)) == 0) {
                UserRole ur = new UserRole();
                ur.setUserId(userId);
                ur.setRoleId(roleId);
                userRoleMapper.insert(ur);
            }
        }
    }

    @Override
    @Transactional
    public void assignRolesToUsers(List<Long> userIds, List<Long> roleIds) {
        if (userIds == null || userIds.isEmpty() || roleIds == null || roleIds.isEmpty()) {
            return;
        }
        for (Long userId : userIds) {
            assignRolesToUser(userId, roleIds);
        }
    }

    @Override
    @Transactional
    public void removeRolesFromUser(Long userId, List<Long> roleIds) {
        if (ADMIN_USER_ID == userId) {
            return; // System admin roles are fixed
        }
        if (roleIds == null || roleIds.isEmpty()) {
            return;
        }
        userRoleMapper.delete(new QueryWrapper<UserRole>().eq("user_id", userId).in("role_id", roleIds));
    }

    @Override
    @Transactional
    public void assignPermissionsToRole(Long roleId, List<Long> permissionIds) {
        // Clear existing
        rolePermissionMapper.delete(new QueryWrapper<RolePermission>().eq("role_id", roleId));
        // Insert new
        for (Long permId : permissionIds) {
            RolePermission rp = new RolePermission();
            rp.setRoleId(roleId);
            rp.setPermissionId(permId);
            rolePermissionMapper.insert(rp);
        }
    }
}
