package com.campus.activity.log.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class LogQueryRequest {
    private Integer page = 1;
    private Integer size = 20;
    private List<String> opTypes;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String keyword;
    private String sortBy;
    private String sortOrder; // asc or desc
}
