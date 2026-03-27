package com.campus.activity.activity.service;

import com.campus.activity.activity.dto.ActivityTrendDto;
import com.campus.activity.activity.dto.ParticipationRankingDto;
import com.campus.activity.activity.dto.StatsOverviewResponse;
import com.campus.activity.activity.entity.Activity;
import com.campus.activity.activity.entity.Registration;
import com.campus.activity.activity.enums.RegistrationStatus;
import com.campus.activity.activity.mapper.ActivityMapper;
import com.campus.activity.activity.mapper.RegistrationMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class StatsServiceTest {

    @Autowired
    private StatsService statsService;

    @Autowired
    private ActivityMapper activityMapper;

    @Autowired
    private RegistrationMapper registrationMapper;

    @BeforeEach
    public void setup() {
        // Create some activities
        Activity a1 = createActivity("Activity 1");
        Activity a2 = createActivity("Activity 2");

        // Create some registrations
        createRegistration(a1.getId(), 2L, RegistrationStatus.APPROVED);
        createRegistration(a1.getId(), 3L, RegistrationStatus.APPROVED);
        createRegistration(a2.getId(), 4L, RegistrationStatus.APPROVED);
    }

    private Activity createActivity(String title) {
        Activity a = new Activity();
        a.setTitle(title);
        a.setStatus("ONLINE");
        a.setStockTotal(100);
        a.setStockAvailable(100);
        a.setCreatedBy(1L);
        a.setUpdatedBy(1L);
        a.setVersion(0);
        a.setCreatedAt(LocalDateTime.now());
        a.setDeleted(0);
        activityMapper.insert(a);
        return a;
    }

    private void createRegistration(Long activityId, Long userId, RegistrationStatus status) {
        Registration r = new Registration();
        r.setActivityId(activityId);
        r.setUserId(userId);
        r.setStatus(status.name());
        r.setCreatedAt(LocalDateTime.now());
        registrationMapper.insert(r);
    }

    @Test
    public void testGetOverview() {
        StatsOverviewResponse overview = statsService.getOverview();
        assertNotNull(overview);
        assertTrue(overview.getTotalActivities() >= 2);
        assertTrue(overview.getTotalRegistrations() >= 3);
    }

    @Test
    public void testGetActivityTrends() {
        LocalDateTime start = LocalDateTime.now().minusDays(7);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        List<ActivityTrendDto> trends = statsService.getActivityTrends(start, end);
        assertNotNull(trends);
        assertFalse(trends.isEmpty());
    }

    @Test
    public void testGetParticipationRanking() {
        List<ParticipationRankingDto> ranking = statsService.getParticipationRanking(10);
        assertNotNull(ranking);
        assertFalse(ranking.isEmpty());
        // Find Activity 1 in the ranking
        ParticipationRankingDto top = ranking.stream()
                .filter(r -> "Activity 1".equals(r.getActivityTitle()))
                .findFirst()
                .orElse(null);
        assertNotNull(top);
        assertEquals(2, top.getRegistrationCount());
    }
}
