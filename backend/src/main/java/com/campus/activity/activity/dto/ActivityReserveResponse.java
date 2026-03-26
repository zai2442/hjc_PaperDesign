package com.campus.activity.activity.dto;

import lombok.Data;

@Data
public class ActivityReserveResponse {
    private Long activityId;
    private Integer remainingStock;
    private String result;

    public static ActivityReserveResponse ok(Long activityId, Integer remainingStock) {
        ActivityReserveResponse resp = new ActivityReserveResponse();
        resp.setActivityId(activityId);
        resp.setRemainingStock(remainingStock);
        resp.setResult("OK");
        return resp;
    }

    public static ActivityReserveResponse fail(Long activityId, String result) {
        ActivityReserveResponse resp = new ActivityReserveResponse();
        resp.setActivityId(activityId);
        resp.setResult(result);
        return resp;
    }
}
