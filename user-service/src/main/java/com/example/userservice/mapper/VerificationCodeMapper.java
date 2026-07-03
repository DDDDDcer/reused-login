package com.example.userservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.userservice.entity.VerificationCode;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface VerificationCodeMapper extends BaseMapper<VerificationCode> {
}