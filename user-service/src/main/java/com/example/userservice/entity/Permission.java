package com.example.userservice.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("permissions")
public class Permission {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String code;

    private String name;

    private String resourceType;

    private String description;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}