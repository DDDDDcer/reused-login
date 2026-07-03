package com.example.msgservice.controller;

import com.example.msgservice.common.ApiResponse;
import com.example.msgservice.service.TemplateService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/message-templates")
@Tag(name = "Message Templates")
public class TemplateController {
    private final TemplateService templateService;

    public TemplateController(TemplateService templateService) {
        this.templateService = templateService;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> list(
            @RequestParam(name = "carrier_type", required = false) String carrierType,
            @RequestParam(required = false) String status) {
        return ApiResponse.ok(templateService.list(carrierType, status));
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        return ApiResponse.ok(templateService.create(
                RequestMaps.required(body, "template_code", "templateCode"),
                RequestMaps.required(body, "template_name", "templateName"),
                RequestMaps.required(body, "template_content", "templateContent"),
                RequestMaps.required(body, "carrier_type", "carrierType"),
                RequestMaps.listOfMaps(body, "variables")));
    }

    @PutMapping("/{templateId}")
    public ApiResponse<Map<String, Object>> update(@PathVariable Long templateId, @RequestBody Map<String, Object> body) {
        List<Map<String, Object>> variables = body.containsKey("variables")
                ? RequestMaps.listOfMaps(body, "variables") : null;
        return ApiResponse.ok(templateService.update(
                templateId,
                RequestMaps.stringValue(body, "template_content", "templateContent"),
                RequestMaps.stringValue(body, "status"),
                variables));
    }

    @GetMapping("/{templateId}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long templateId) {
        return ApiResponse.ok(templateService.detail(templateId));
    }

    @DeleteMapping("/{templateId}")
    public ApiResponse<Map<String, Object>> delete(@PathVariable Long templateId) {
        return ApiResponse.ok(templateService.disable(templateId));
    }
}
