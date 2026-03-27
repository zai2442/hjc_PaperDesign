package com.campus.activity.activity.controller;

import com.campus.activity.activity.dto.ActivityTrendDto;
import com.campus.activity.activity.dto.ParticipationRankingDto;
import com.campus.activity.activity.dto.StatsOverviewResponse;
import com.campus.activity.activity.service.StatsService;
import com.campus.activity.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/stats")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN','COUNSELOR')")
public class StatsAdminController {

    private final StatsService statsService;

    @GetMapping("/overview")
    public Result<StatsOverviewResponse> getOverview() {
        return Result.success(statsService.getOverview());
    }

    @GetMapping("/trends")
    public Result<List<ActivityTrendDto>> getTrends(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return Result.success(statsService.getActivityTrends(start, end));
    }

    @GetMapping("/ranking")
    public Result<List<ParticipationRankingDto>> getRanking(
            @RequestParam(defaultValue = "10") int limit) {
        return Result.success(statsService.getParticipationRanking(limit));
    }
}
