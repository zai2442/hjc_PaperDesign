package com.campus.activity.user.controller;

import com.campus.activity.common.Result;
import com.campus.activity.user.dto.UserRoleAssignDto;
import com.campus.activity.user.entity.Role;
import com.campus.activity.user.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public Result<List<Role>> listRoles() {
        return Result.success(roleService.list());
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public Result<Role> createRole(@RequestBody Role role) {
        roleService.save(role);
        return Result.success(role);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public Result<Void> updateRole(@PathVariable Long id, @RequestBody Role role) {
        role.setId(id);
        roleService.updateById(role);
        return Result.success(null);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public Result<Void> deleteRole(@PathVariable Long id) {
        roleService.removeById(id);
        return Result.success(null);
    }

    @PostMapping("/assign")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public Result<Void> assignRoles(@RequestBody UserRoleAssignDto dto) {
        roleService.assignRolesToUser(dto.getUserId(), dto.getRoleIds());
        return Result.success(null);
    }

    @PostMapping("/remove")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public Result<Void> removeRoles(@RequestBody UserRoleAssignDto dto) {
        roleService.removeRolesFromUser(dto.getUserId(), dto.getRoleIds());
        return Result.success(null);
    }
}
