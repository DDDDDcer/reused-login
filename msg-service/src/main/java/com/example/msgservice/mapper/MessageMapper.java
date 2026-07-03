package com.example.msgservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.msgservice.entity.Message;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MessageMapper extends BaseMapper<Message> {
}
