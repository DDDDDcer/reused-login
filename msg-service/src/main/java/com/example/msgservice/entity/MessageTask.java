package com.example.msgservice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("message_tasks")
public class MessageTask {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long messageId;
    private Long carrierId;
    private Long accountId;
    private Long strategyId;
    private String carrierType;
    private String receiverId;
    private String status;
    private String bizCode;
    private String sceneCode;
    private String idempotencyKey;
    private String requestHash;
    private Integer retryCount;
    private Integer localDeleted;
    private LocalDateTime plannedTime;
    private LocalDateTime nextRetryTime;
    private LocalDateTime sentAt;
    private String failReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
