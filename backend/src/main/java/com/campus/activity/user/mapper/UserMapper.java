package com.campus.activity.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.activity.user.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
