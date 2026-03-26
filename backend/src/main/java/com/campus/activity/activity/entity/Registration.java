package com.campus.activity.activity.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("act_registration")
public class Registration {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("activity_id")
    private Long activityId;

    @TableField("user_id")
    private Long userId;

    private String status;

    @TableField("extra_data")
    private String extraData;

    @TableField("audit_reason")
    private String auditReason;

    @TableField("audit_by")
    private Long auditBy;

    @TableField("audit_at")
    private LocalDateTime auditAt;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
