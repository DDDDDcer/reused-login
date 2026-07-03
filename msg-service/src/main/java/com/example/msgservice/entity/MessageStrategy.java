package com.example.msgservice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("message_strategies")
public class MessageStrategy {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String strategyName;
    private Integer maxRetries;
    private Integer retryIntervalSeconds;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
