package com.campus.activity.activity.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Data
public class ActivityUpdateRequest {

    @NotNull(message = "version is required")
    private Integer version;

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
    private String formSchema;
    private String channels;
    private java.util.List<Long> tagIds;
    private Integer whitelistEnabled;
    private Integer stockTotal;
    private Integer perUserLimit;
    private String currentVariant;
    private LocalDateTime publishAt;
    private LocalDateTime offlineAt;
}
