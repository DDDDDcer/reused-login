package com.example.userservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.userservice.entity.Credential;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CredentialMapper extends BaseMapper<Credential> {
}