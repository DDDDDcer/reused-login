package com.example.userservice.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("group_permissions")
public class GroupPermission {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long groupId;

    private Long permissionId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}