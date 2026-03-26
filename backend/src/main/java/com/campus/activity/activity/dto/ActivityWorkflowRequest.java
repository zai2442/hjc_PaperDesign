package com.campus.activity.activity.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Data
public class ActivityWorkflowRequest {

    @NotNull(message = "version is required")
    private Integer version;

    private LocalDateTime publishAt;
    private LocalDateTime offlineAt;

    private String reason;
}
