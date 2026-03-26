package com.campus.activity.activity.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("act_activity_change_log")
public class ActivityChangeLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("activity_id")
    private Long activityId;

    @TableField("operator_id")
    private Long operatorId;

    @TableField("op_type")
    private String opType;

    @TableField("before_data")
    private String beforeData;

    @TableField("after_data")
    private String afterData;

    @TableField("diff_data")
    private String diffData;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
