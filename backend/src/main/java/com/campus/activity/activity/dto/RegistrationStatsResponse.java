package com.campus.activity.activity.dto;

import lombok.Data;

@Data
public class RegistrationStatsResponse {
    private Long activityId;
    private long total;
    private long pending;
    private long approved;
    private long rejected;
    private long canceled;
    private long completed;
}
