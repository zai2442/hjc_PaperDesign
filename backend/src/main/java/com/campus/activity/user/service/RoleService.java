package com.campus.activity.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.campus.activity.user.entity.Role;

import java.util.List;

public interface RoleService extends IService<Role> {
    void assignRolesToUser(Long userId, List<Long> roleIds);
    void assignRolesToUsers(List<Long> userIds, List<Long> roleIds);
    void removeRolesFromUser(Long userId, List<Long> roleIds);
    void assignPermissionsToRole(Long roleId, List<Long> permissionIds);
}
