package com.example.userservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.userservice.entity.Permission;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PermissionMapper extends BaseMapper<Permission> {
}