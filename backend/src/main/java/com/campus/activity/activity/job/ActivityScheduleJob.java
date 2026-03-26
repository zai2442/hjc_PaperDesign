package com.campus.activity.activity.job;

import com.campus.activity.activity.service.ActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Profile("!test")
public class ActivityScheduleJob {

    private final ActivityService activityService;

    @Scheduled(fixedDelayString = "30000")
    public void tick() {
        activityService.runScheduleTick();
    }
}
