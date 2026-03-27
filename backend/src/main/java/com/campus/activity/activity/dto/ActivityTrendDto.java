package com.campus.activity.activity.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ActivityTrendDto {
    private String date; // YYYY-MM-DD, YYYY-WW, or YYYY-MM
    private long count;
}
