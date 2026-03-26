package com.campus.activity.activity.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class ActivityVariantUpsertRequest {

    @NotBlank(message = "variantCode is required")
    private String variantCode;

    private String title;
    private String summary;
    private String coverUrl;
    private String content;
    private String contentType;
}
