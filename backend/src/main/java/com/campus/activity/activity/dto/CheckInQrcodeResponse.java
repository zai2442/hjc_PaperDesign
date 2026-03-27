package com.campus.activity.activity.dto;

import lombok.Data;

@Data
public class CheckInQrcodeResponse {
    private String token;
    private Long expireAt; // timestamp
}
