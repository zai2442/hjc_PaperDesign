package com.campus.activity.activity.dto;

import lombok.Data;

@Data
public class RegistrationAuditRequest {
    private String status; // APPROVED, REJECTED
    private String reason;
}
