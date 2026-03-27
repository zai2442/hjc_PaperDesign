package com.campus.activity.log.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.activity.common.PageResponse;
import com.campus.activity.log.dto.LogQueryRequest;
import com.campus.activity.log.entity.OperationLog;
import com.campus.activity.log.mapper.OperationLogMapper;
import com.campus.activity.log.service.OperationLogService;
import com.campus.activity.log.websocket.LogWebSocketHandler;
import com.campus.activity.security.LoginUser;
import com.campus.activity.security.SecurityUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OperationLogServiceImpl extends ServiceImpl<OperationLogMapper, OperationLog> implements OperationLogService {

    private final LogWebSocketHandler logWebSocketHandler;
    private final ObjectMapper objectMapper;

    @Override
    public PageResponse<OperationLog> queryLogs(LogQueryRequest request) {
        Page<OperationLog> page = new Page<>(request.getPage(), request.getSize());
        LambdaQueryWrapper<OperationLog> queryWrapper = new LambdaQueryWrapper<>();

        // 操作类型筛选
        if (request.getOpTypes() != null && !request.getOpTypes().isEmpty()) {
            queryWrapper.in(OperationLog::getOpType, request.getOpTypes());
        }

        // 时间范围查询
        if (request.getStartTime() != null) {
            queryWrapper.ge(OperationLog::getCreatedAt, request.getStartTime());
        }
        if (request.getEndTime() != null) {
            queryWrapper.le(OperationLog::getCreatedAt, request.getEndTime());
        }

        // 关键字搜索 (活动ID、活动名称、操作人账号、操作人昵称)
        if (StringUtils.hasText(request.getKeyword())) {
            String keyword = request.getKeyword();
            queryWrapper.and(wrapper -> wrapper
                    .like(OperationLog::getActivityId, keyword)
                    .or().like(OperationLog::getActivityTitle, keyword)
                    .or().like(OperationLog::getOperatorUsername, keyword)
                    .or().like(OperationLog::getOperatorNickname, keyword));
        }

        // 排序
        if (StringUtils.hasText(request.getSortBy())) {
            boolean isAsc = "asc".equalsIgnoreCase(request.getSortOrder());
            // Map camelCase to snake_case for MybatisPlus or use column names directly
            // For LambdaQueryWrapper, we need to handle mapping if sortBy is from frontend
            switch (request.getSortBy()) {
                case "createdAt": queryWrapper.orderBy(true, isAsc, OperationLog::getCreatedAt); break;
                case "operatorUsername": queryWrapper.orderBy(true, isAsc, OperationLog::getOperatorUsername); break;
                case "activityId": queryWrapper.orderBy(true, isAsc, OperationLog::getActivityId); break;
                case "activityTitle": queryWrapper.orderBy(true, isAsc, OperationLog::getActivityTitle); break;
                case "opType": queryWrapper.orderBy(true, isAsc, OperationLog::getOpType); break;
                case "opResult": queryWrapper.orderBy(true, isAsc, OperationLog::getOpResult); break;
                default: queryWrapper.orderByDesc(OperationLog::getCreatedAt);
            }
        } else {
            queryWrapper.orderByDesc(OperationLog::getCreatedAt);
        }

        baseMapper.selectPage(page, queryWrapper);
        return PageResponse.from(page);
    }

    @Override
    public void log(OperationLog log) {
        if (log.getCreatedAt() == null) {
            log.setCreatedAt(LocalDateTime.now());
        }
        this.save(log);
        // 通知 WebSocket 有新日志
        logWebSocketHandler.notifyNewLog();
    }

    @Override
    public void log(String opType, Long activityId, String activityTitle, Object detail, boolean success, String errorMsg) {
        OperationLog log = new OperationLog();
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (loginUser != null) {
            log.setOperatorId(loginUser.getUser().getId());
            log.setOperatorUsername(loginUser.getUsername());
            log.setOperatorNickname(loginUser.getUser().getNickname());
        } else {
            log.setOperatorId(0L);
            log.setOperatorUsername("system");
            log.setOperatorNickname("系统");
        }
        log.setActivityId(activityId);
        log.setActivityTitle(activityTitle);
        log.setOpType(opType);
        try {
            log.setOpDetail(objectMapper.writeValueAsString(detail));
        } catch (JsonProcessingException e) {
            log.setOpDetail("{}");
        }
        log.setOpResult(success ? 1 : 0);
        log.setErrorMsg(errorMsg);
        log.setCreatedAt(LocalDateTime.now());
        this.log(log);
    }
}
