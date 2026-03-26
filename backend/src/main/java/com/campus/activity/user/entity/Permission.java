package com.campus.activity.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_permission")
public class Permission {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String permName;
    private String permCode;
    private Integer type;
    private Long parentId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
