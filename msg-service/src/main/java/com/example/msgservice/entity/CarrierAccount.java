package com.example.msgservice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("carrier_accounts")
public class CarrierAccount {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long carrierId;
    private String accountName;
    private String provider;
    private String accessKey;
    private String configJson;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
