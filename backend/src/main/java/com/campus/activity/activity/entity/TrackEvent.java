package com.campus.activity.activity.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("act_track_event")
public class TrackEvent {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("event_name")
    private String eventName;

    @TableField("user_id")
    private Long userId;

    @TableField("activity_id")
    private Long activityId;

    @TableField("event_data")
    private String eventData;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
