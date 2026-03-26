package com.campus.activity.activity.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ActivityQueryRequest {
    private long page = 1;
    private long size = 10;

    private String keyword;
    private String status;
    private Long createdBy;

    private LocalDateTime createdFrom;
    private LocalDateTime createdTo;
}
