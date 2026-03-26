package com.campus.activity.user.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;

@Data
public class LoginDto {
    @NotBlank(message = "Username cannot be blank")
    private String username;

    @NotBlank(message = "Password cannot be blank")
    private String password;
}
