package com.campus.activity.activity.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.activity.activity.entity.Activity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Delete;

@Mapper
public interface ActivityMapper extends BaseMapper<Activity> {
    @Insert("INSERT INTO act_activity_tag (activity_id, tag_id) VALUES (#{activityId}, #{tagId})")
    void insertActivityTag(@Param("activityId") Long activityId, @Param("tagId") Long tagId);

    @Delete("DELETE FROM act_activity_tag WHERE activity_id = #{activityId}")
    void deleteActivityTags(@Param("activityId") Long activityId);
}

