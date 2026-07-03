package com.example.msgservice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("message_logs")
public class MessageLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private Integer attemptNo;
    private String status;
    private String providerResponse;
    private String failReason;
    private LocalDateTime executedAt;
}
