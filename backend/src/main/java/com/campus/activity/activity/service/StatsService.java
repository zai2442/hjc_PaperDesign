package com.campus.activity.activity.service;

import com.campus.activity.activity.dto.ActivityTrendDto;
import com.campus.activity.activity.dto.ParticipationRankingDto;
import com.campus.activity.activity.dto.StatsOverviewResponse;
import java.util.List;
import java.time.LocalDateTime;

public interface StatsService {
    StatsOverviewResponse getOverview();
    List<ActivityTrendDto> getActivityTrends(LocalDateTime start, LocalDateTime end);
    List<ParticipationRankingDto> getParticipationRanking(int limit);
}
