package com.campus.activity.activity.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class TrackEventRequest {

    @NotBlank(message = "eventName is required")
    private String eventName;

    private Long activityId;
    private String eventData;
}
