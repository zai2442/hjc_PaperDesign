package com.campus.activity.activity.controller;

import com.campus.activity.activity.dto.CheckInManualRequest;
import com.campus.activity.activity.dto.CheckInQrcodeResponse;
import com.campus.activity.activity.dto.CheckInScanRequest;
import com.campus.activity.activity.dto.CheckInStatsResponse;
import com.campus.activity.activity.service.CheckInService;
import com.campus.activity.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/activities/{id}/checkin")
@RequiredArgsConstructor
public class CheckInController {

    private final CheckInService checkInService;

    @PostMapping("/start")
    public Result<Void> startCheckIn(@PathVariable Long id) {
        checkInService.startCheckIn(id);
        return Result.success(null);
    }

    @PostMapping("/stop")
    public Result<Void> stopCheckIn(@PathVariable Long id) {
        checkInService.stopCheckIn(id);
        return Result.success(null);
    }

    @GetMapping("/qrcode")
    public Result<CheckInQrcodeResponse> getQrcode(@PathVariable Long id) {
        return Result.success(checkInService.getQrcode(id));
    }

    @PostMapping("/scan")
    public Result<Void> scanQrcode(@PathVariable Long id, @RequestBody CheckInScanRequest req) {
        checkInService.scanQrcode(id, req);
        return Result.success(null);
    }

    @PostMapping("/manual")
    public Result<Void> manualCheckIn(@PathVariable Long id, @RequestBody CheckInManualRequest req) {
        checkInService.manualCheckIn(id, req.getUserId());
        return Result.success(null);
    }

    @GetMapping("/stats")
    public Result<CheckInStatsResponse> getStats(@PathVariable Long id) {
        return Result.success(checkInService.getStats(id));
    }
}
