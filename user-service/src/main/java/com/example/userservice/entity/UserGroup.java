package com.example.userservice.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_groups")
public class UserGroup {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long groupId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}