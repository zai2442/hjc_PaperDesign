package com.campus.activity.user.dto;

import com.campus.activity.user.entity.Role;
import com.campus.activity.user.entity.User;
import lombok.Data;

import java.util.List;

@Data
public class UserInfoDto {
    private Long id;
    private String username;
    private String nickname;
    private String avatarUrl;
    private String email;
    private String phone;
    private List<Role> roles;

    public static UserInfoDto from(User user, List<Role> roles) {
        UserInfoDto dto = new UserInfoDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setNickname(user.getNickname());
        dto.setAvatarUrl(user.getAvatarUrl());
        dto.setEmail(user.getEmail());
        dto.setPhone(user.getPhone());
        dto.setRoles(roles);
        return dto;
    }
}
