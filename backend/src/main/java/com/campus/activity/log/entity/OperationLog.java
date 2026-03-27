package com.campus.activity.log.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_operation_log")
public class OperationLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long operatorId;
    private String operatorUsername;
    private String operatorNickname;
    private Long activityId;
    private String activityTitle;
    private String opType;
    private String opDetail;
    private Integer opResult;
    private String errorMsg;
    private LocalDateTime createdAt;
}
