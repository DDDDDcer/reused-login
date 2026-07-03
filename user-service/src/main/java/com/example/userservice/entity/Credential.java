package com.example.userservice.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("credentials")
public class Credential {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String credentialType;

    private String credentialValue;

    private String passwordHash;

    private String salt;

    private Integer isPrimary;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}