package com.campus.activity.activity.controller;

import com.campus.activity.activity.entity.Tag;
import com.campus.activity.activity.mapper.TagMapper;
import com.campus.activity.common.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/tags")
public class TagController {

    @Autowired
    private TagMapper tagMapper;

    @GetMapping
    public Result<List<Tag>> getAllTags() {
        return Result.success(tagMapper.selectList(null));
    }
}
