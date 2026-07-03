package com.example.userservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.userservice.entity.UserAttributeValue;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserAttributeValueMapper extends BaseMapper<UserAttributeValue> {
}