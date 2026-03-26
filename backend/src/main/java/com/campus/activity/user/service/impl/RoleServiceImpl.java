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

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl extends ServiceImpl<RoleMapper, Role> implements RoleService {

    private static final long ADMIN_USER_ID = 1L;
    private static final long SUPER_ADMIN_ROLE_ID = 1L;

    private final UserRoleMapper userRoleMapper;
    private final RolePermissionMapper rolePermissionMapper;

    @Override
    @Transactional
    public void assignRolesToUser(Long userId, List<Long> roleIds) {
        if (ADMIN_USER_ID == userId) {
            ensureAdminIsSuperAdmin();
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
        if (ADMIN_USER_ID == userId) {
            ensureAdminIsSuperAdmin();
        }
    }

    @Override
    @Transactional
    public void removeRolesFromUser(Long userId, List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return;
        }
        if (ADMIN_USER_ID == userId) {
            List<Long> filtered = new ArrayList<>();
            for (Long roleId : roleIds) {
                if (roleId != null && roleId != SUPER_ADMIN_ROLE_ID) {
                    filtered.add(roleId);
                }
            }
            if (filtered.isEmpty()) {
                ensureAdminIsSuperAdmin();
                return;
            }
            userRoleMapper.delete(new QueryWrapper<UserRole>().eq("user_id", userId).in("role_id", filtered));
            ensureAdminIsSuperAdmin();
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

    private void ensureAdminIsSuperAdmin() {
        if (userRoleMapper.selectCount(
                new QueryWrapper<UserRole>().eq("user_id", ADMIN_USER_ID).eq("role_id", SUPER_ADMIN_ROLE_ID)
        ) == 0) {
            UserRole ur = new UserRole();
            ur.setUserId(ADMIN_USER_ID);
            ur.setRoleId(SUPER_ADMIN_ROLE_ID);
            userRoleMapper.insert(ur);
        }
    }
}
