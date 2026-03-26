package com.campus.activity.activity.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.time.LocalDateTime;

@Data
public class ActivityCreateRequest {

    @NotBlank(message = "title is required")
    private String title;

    private String summary;
    private String coverUrl;
    private String content;
    private String contentType;
    private String location;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime regStartTime;
    private LocalDateTime regEndTime;
    private LocalDateTime publishAt;
    private LocalDateTime offlineAt;
    private String formSchema;
    private String channels;
    private Integer whitelistEnabled;
    private Integer stockTotal;
    private Integer perUserLimit;
    private java.util.List<Long> tagIds;
}
