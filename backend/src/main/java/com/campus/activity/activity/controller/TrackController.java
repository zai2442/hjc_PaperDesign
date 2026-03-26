package com.campus.activity.activity.controller;

import com.campus.activity.activity.dto.TrackEventRequest;
import com.campus.activity.activity.entity.TrackEvent;
import com.campus.activity.activity.mapper.TrackEventMapper;
import com.campus.activity.common.Result;
import com.campus.activity.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/track")
@RequiredArgsConstructor
@Validated
public class TrackController {

    private final TrackEventMapper trackEventMapper;

    @PostMapping
    public Result<Void> track(@Valid @RequestBody TrackEventRequest req) {
        TrackEvent e = new TrackEvent();
        e.setEventName(req.getEventName());
        e.setActivityId(req.getActivityId());
        e.setEventData(req.getEventData());
        e.setUserId(SecurityUtils.getUserId());
        e.setCreatedAt(LocalDateTime.now());
        trackEventMapper.insert(e);
        return Result.success(null);
    }
}

