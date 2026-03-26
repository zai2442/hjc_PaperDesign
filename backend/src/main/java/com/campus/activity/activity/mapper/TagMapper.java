package com.campus.activity.activity.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.activity.activity.entity.Tag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface TagMapper extends BaseMapper<Tag> {
    @Select("SELECT t.* FROM act_tag t JOIN act_activity_tag at ON t.id = at.tag_id WHERE at.activity_id = #{activityId}")
    List<Tag> findTagsByActivityId(Long activityId);
}
