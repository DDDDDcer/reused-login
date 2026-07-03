package com.example.userservice.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_permissions")
public class UserPermission {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long permissionId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}