package com.example.userservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.userservice.entity.UserPermission;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserPermissionMapper extends BaseMapper<UserPermission> {
}