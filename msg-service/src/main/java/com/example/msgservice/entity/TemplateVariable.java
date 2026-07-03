package com.example.msgservice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("template_variables")
public class TemplateVariable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long templateId;
    private String variableName;
    private String description;
    private Integer required;
    private String defaultValue;
    private LocalDateTime createdAt;
}
