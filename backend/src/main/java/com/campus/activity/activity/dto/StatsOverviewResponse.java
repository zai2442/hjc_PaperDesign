package com.campus.activity.activity.dto;

import lombok.Data;

@Data
public class StatsOverviewResponse {
    private long totalActivities;
    private long totalRegistrations;
    private long totalCheckIns;
    private double overallCheckInRate;
}
