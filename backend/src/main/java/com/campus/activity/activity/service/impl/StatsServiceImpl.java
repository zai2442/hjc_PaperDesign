package com.campus.activity.activity.service.impl;

import com.campus.activity.activity.dto.ActivityTrendDto;
import com.campus.activity.activity.dto.ParticipationRankingDto;
import com.campus.activity.activity.dto.StatsOverviewResponse;
import com.campus.activity.activity.mapper.StatsMapper;
import com.campus.activity.activity.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class StatsServiceImpl implements StatsService {

    private final StatsMapper statsMapper;

    @Override
    public StatsOverviewResponse getOverview() {
        StatsOverviewResponse overview = new StatsOverviewResponse();
        overview.setTotalActivities(statsMapper.countTotalActivities());
        overview.setTotalRegistrations(statsMapper.countTotalRegistrations());
        overview.setTotalCheckIns(statsMapper.countTotalCheckIns());
        
        if (overview.getTotalRegistrations() > 0) {
            overview.setOverallCheckInRate((double) overview.getTotalCheckIns() / overview.getTotalRegistrations());
        } else {
            overview.setOverallCheckInRate(0.0);
        }
        
        return overview;
    }

    @Override
    public List<ActivityTrendDto> getActivityTrends(LocalDateTime start, LocalDateTime end) {
        return statsMapper.getActivityTrendsByDay(start, end);
    }

    @Override
    public List<ParticipationRankingDto> getParticipationRanking(int limit) {
        List<ParticipationRankingDto> ranking = statsMapper.getParticipationRanking(limit);
        for (ParticipationRankingDto dto : ranking) {
            if (dto.getRegistrationCount() > 0) {
                dto.setCheckInRate((double) dto.getCheckInCount() / dto.getRegistrationCount());
            } else {
                dto.setCheckInRate(0.0);
            }
        }
        return ranking;
    }
}
