package com.campus.activity.activity.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CheckInStatsResponse {
    private long totalRegistered;
    private long checkedIn;
    private List<RecordDto> records;

    @Data
    public static class RecordDto {
        private Long userId;
        private String username;
        private LocalDateTime checkInTime;
        private String type; // SCAN or MANUAL
        private boolean checkedIn;
    }
}
