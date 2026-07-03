package com.example.userservice.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("verification_codes")
public class VerificationCode {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String target;

    private String scene;

    private String code;

    private Integer used;

    private LocalDateTime expiresAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}