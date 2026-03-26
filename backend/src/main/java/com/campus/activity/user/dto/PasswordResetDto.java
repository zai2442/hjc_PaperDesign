package com.campus.activity.user.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;

@Data
public class PasswordResetDto {
    @NotBlank(message = "Old password cannot be blank")
    private String oldPassword;

    @NotBlank(message = "New password cannot be blank")
    private String newPassword;
}
