package com.campus.activity.activity.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("act_activity")
public class Activity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String summary;

    @TableField("cover_url")
    private String coverUrl;

    private String content;

    @TableField("content_type")
    private String contentType;
    
    private String location;

    @TableField("start_time")
    private LocalDateTime startTime;

    @TableField("end_time")
    private LocalDateTime endTime;

    @TableField("reg_start_time")
    private LocalDateTime regStartTime;

    @TableField("reg_end_time")
    private LocalDateTime regEndTime;

    @TableField("form_schema")
    private String formSchema;

    private String channels;

    @TableField("whitelist_enabled")
    private Integer whitelistEnabled;

    @TableField("stock_total")
    private Integer stockTotal;

    @TableField("stock_available")
    private Integer stockAvailable;

    @TableField("per_user_limit")
    private Integer perUserLimit;

    @TableField("current_variant")
    private String currentVariant;

    private String status;

    @TableField("audit_reason")
    private String auditReason;

    @TableField("audit_by")
    private Long auditBy;

    @TableField("audit_at")
    private LocalDateTime auditAt;

    @TableField("publish_at")
    private LocalDateTime publishAt;

    @TableField("offline_at")
    private LocalDateTime offlineAt;

    @TableField("created_by")
    private Long createdBy;

    @TableField("updated_by")
    private Long updatedBy;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    @Version
    private Integer version;

    @TableLogic
    private Integer deleted;
}
