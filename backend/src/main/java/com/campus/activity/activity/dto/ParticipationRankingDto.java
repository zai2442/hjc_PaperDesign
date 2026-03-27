package com.campus.activity.activity.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ParticipationRankingDto {
    private Long activityId;
    private String activityTitle;
    private long registrationCount;
    private long checkInCount;
    private double checkInRate;
}
