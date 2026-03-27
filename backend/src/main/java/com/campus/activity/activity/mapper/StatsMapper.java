package com.campus.activity.activity.mapper;

import com.campus.activity.activity.dto.ActivityTrendDto;
import com.campus.activity.activity.dto.ParticipationRankingDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;
import java.time.LocalDateTime;

@Mapper
public interface StatsMapper {

    @Select("SELECT COUNT(*) FROM act_activity WHERE deleted = 0")
    long countTotalActivities();

    @Select("SELECT COUNT(*) FROM act_registration WHERE status IN ('APPROVED', 'COMPLETED')")
    long countTotalRegistrations();

    @Select("SELECT COUNT(*) FROM act_check_in_record")
    long countTotalCheckIns();

    @Select("SELECT LEFT(created_at, 10) as date, COUNT(*) as count " +
            "FROM act_activity " +
            "WHERE deleted = 0 AND created_at BETWEEN #{start} AND #{end} " +
            "GROUP BY date ORDER BY date ASC")
    List<ActivityTrendDto> getActivityTrendsByDay(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Select("SELECT a.id as activityId, a.title as activityTitle, " +
            "COUNT(r.id) as registrationCount, " +
            "(SELECT COUNT(*) FROM act_check_in_record c WHERE c.activity_id = a.id) as checkInCount " +
            "FROM act_activity a " +
            "LEFT JOIN act_registration r ON a.id = r.activity_id AND r.status IN ('APPROVED', 'COMPLETED') " +
            "WHERE a.deleted = 0 " +
            "GROUP BY a.id, a.title " +
            "ORDER BY registrationCount DESC " +
            "LIMIT #{limit}")
    List<ParticipationRankingDto> getParticipationRanking(@Param("limit") int limit);
}
