package com.campus.activity.user.dto;

import lombok.Data;
import java.util.List;

@Data
public class UserRoleAssignDto {
    private Long userId;
    private List<Long> userIds;
    private List<Long> roleIds;
}
