package com.example.msgservice.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.msgservice.common.BusinessException;
import com.example.msgservice.entity.MessageTemplate;
import com.example.msgservice.entity.TemplateVariable;
import com.example.msgservice.mapper.MessageTemplateMapper;
import com.example.msgservice.mapper.TemplateVariableMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class TemplateService {
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{\\s*([a-zA-Z0-9_]+)\\s*}}");

    private final MessageTemplateMapper templateMapper;
    private final TemplateVariableMapper variableMapper;

    public TemplateService(MessageTemplateMapper templateMapper, TemplateVariableMapper variableMapper) {
        this.templateMapper = templateMapper;
        this.variableMapper = variableMapper;
    }

    public Map<String, Object> list(String carrierType, String status) {
        LambdaQueryWrapper<MessageTemplate> wrapper = new LambdaQueryWrapper<>();
        if (carrierType != null && !carrierType.isBlank()) {
            wrapper.eq(MessageTemplate::getCarrierType, carrierType.toUpperCase());
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq(MessageTemplate::getStatus, status.toUpperCase());
        }
        List<MessageTemplate> templates = templateMapper.selectList(wrapper.orderByDesc(MessageTemplate::getCreatedAt));
        return Map.of("template_list", templates, "total", templates.size());
    }

    @Transactional
    public Map<String, Object> create(String code, String name, String content, String carrierType,
                                      List<Map<String, Object>> variables) {
        if (templateMapper.selectCount(new LambdaQueryWrapper<MessageTemplate>()
                .eq(MessageTemplate::getTemplateCode, code)) > 0) {
            throw new BusinessException(409, "template_code already exists");
        }
        validateDefinitions(content, variables);
        MessageTemplate template = new MessageTemplate();
        template.setTemplateCode(code);
        template.setTemplateName(name);
        template.setTemplateContent(content);
        template.setCarrierType(carrierType.toUpperCase());
        template.setStatus("ENABLED");
        templateMapper.insert(template);
        replaceVariables(template.getId(), variables);
        return Map.of("template_id", template.getId());
    }

    @Transactional
    public Map<String, Object> update(Long id, String content, String status, List<Map<String, Object>> variables) {
        MessageTemplate template = requireTemplate(id);
        String effectiveContent = content == null ? template.getTemplateContent() : content;
        List<Map<String, Object>> effectiveVariables = variables == null
                ? variableDefinitions(id) : variables;
        validateDefinitions(effectiveContent, effectiveVariables);
        if (content != null) template.setTemplateContent(content);
        if (status != null) template.setStatus(status.toUpperCase());
        templateMapper.updateById(template);
        if (variables != null) replaceVariables(id, variables);
        return Map.of("updated", true);
    }

    public Map<String, Object> detail(Long id) {
        MessageTemplate template = requireTemplate(id);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("template_info", template);
        result.put("variables", variableMapper.selectList(new LambdaQueryWrapper<TemplateVariable>()
                .eq(TemplateVariable::getTemplateId, id)));
        return result;
    }

    @Transactional
    public Map<String, Object> disable(Long id) {
        MessageTemplate template = requireTemplate(id);
        template.setStatus("DISABLED");
        templateMapper.updateById(template);
        return Map.of("deleted", true, "mode", "soft-disable");
    }

    public String render(Long templateId, Map<String, Object> values, String expectedCarrierType) {
        MessageTemplate template = requireTemplate(templateId);
        if (!"ENABLED".equalsIgnoreCase(template.getStatus())) {
            throw new BusinessException(400, "template is disabled");
        }
        if (!template.getCarrierType().equalsIgnoreCase(expectedCarrierType)) {
            throw new BusinessException(400, "template carrier_type does not match request");
        }
        String content = template.getTemplateContent();
        for (TemplateVariable variable : variableMapper.selectList(new LambdaQueryWrapper<TemplateVariable>()
                .eq(TemplateVariable::getTemplateId, templateId))) {
            Object value = values.get(variable.getVariableName());
            if (value == null) value = variable.getDefaultValue();
            if (value == null && Integer.valueOf(1).equals(variable.getRequired())) {
                throw new BusinessException(400, "required template variable missing: " + variable.getVariableName());
            }
            content = content.replaceAll("\\{\\{\\s*" + Pattern.quote(variable.getVariableName()) + "\\s*}}",
                    Matcher.quoteReplacement(value == null ? "" : String.valueOf(value)));
        }
        return content;
    }

    private MessageTemplate requireTemplate(Long id) {
        MessageTemplate template = templateMapper.selectById(id);
        if (template == null) throw new BusinessException(404, "template not found: " + id);
        return template;
    }

    private void validateDefinitions(String content, List<Map<String, Object>> variables) {
        Set<String> placeholders = new LinkedHashSet<>();
        Matcher matcher = PLACEHOLDER.matcher(content);
        while (matcher.find()) placeholders.add(matcher.group(1));
        Set<String> definitions = variables.stream()
                .map(v -> String.valueOf(v.getOrDefault("variable_name", v.get("name"))))
                .filter(v -> !"null".equals(v))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (!placeholders.equals(definitions)) {
            throw new BusinessException(400,
                    "template variables must exactly match placeholders; placeholders=" + placeholders
                            + ", definitions=" + definitions);
        }
    }

    private List<Map<String, Object>> variableDefinitions(Long templateId) {
        return variableMapper.selectList(new LambdaQueryWrapper<TemplateVariable>()
                        .eq(TemplateVariable::getTemplateId, templateId)).stream()
                .map(v -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("variable_name", v.getVariableName());
                    row.put("description", v.getDescription());
                    row.put("required", v.getRequired());
                    row.put("default_value", v.getDefaultValue());
                    return row;
                }).toList();
    }

    private void replaceVariables(Long templateId, List<Map<String, Object>> variables) {
        variableMapper.delete(new LambdaQueryWrapper<TemplateVariable>()
                .eq(TemplateVariable::getTemplateId, templateId));
        for (Map<String, Object> row : variables) {
            TemplateVariable variable = new TemplateVariable();
            variable.setTemplateId(templateId);
            Object rawName = row.getOrDefault("variable_name", row.get("name"));
            variable.setVariableName(String.valueOf(rawName));
            variable.setDescription(row.get("description") == null ? null : String.valueOf(row.get("description")));
            Object required = row.get("required");
            variable.setRequired(required == null || Boolean.TRUE.equals(required) || "1".equals(String.valueOf(required)) ? 1 : 0);
            variable.setDefaultValue(row.get("default_value") == null ? null : String.valueOf(row.get("default_value")));
            variableMapper.insert(variable);
        }
    }
}
