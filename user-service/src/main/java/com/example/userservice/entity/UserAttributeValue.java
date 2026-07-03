package com.example.userservice.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_attribute_values")
public class UserAttributeValue {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String attrKey;

    private String attrValue;

    private String valueStr;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}