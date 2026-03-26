package com.campus.activity.activity.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("act_activity_variant")
public class ActivityVariant {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("activity_id")
    private Long activityId;

    @TableField("variant_code")
    private String variantCode;

    @TableField("variant_version")
    private Integer variantVersion;

    private String title;

    private String summary;

    @TableField("cover_url")
    private String coverUrl;

    private String content;

    @TableField("content_type")
    private String contentType;

    @TableField("created_by")
    private Long createdBy;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
