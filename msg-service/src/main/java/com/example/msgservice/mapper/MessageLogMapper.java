package com.example.msgservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.msgservice.entity.MessageLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MessageLogMapper extends BaseMapper<MessageLog> {
}
