package com.example.userservice.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("attribute_definitions")
public class AttributeDefinition {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String attrKey;

    private String attrName;

    private String attrType;

    private Integer isHot;

    private String description;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}