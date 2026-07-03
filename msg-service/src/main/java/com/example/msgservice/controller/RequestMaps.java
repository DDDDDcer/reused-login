package com.example.msgservice.controller;

import com.example.msgservice.common.BusinessException;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class RequestMaps {
    private RequestMaps() {
    }

    static String stringValue(Map<String, Object> body, String... keys) {
        if (body == null) return null;
        for (String key : keys) {
            Object value = body.get(key);
            if (value != null) return String.valueOf(value);
        }
        return null;
    }

    static String required(Map<String, Object> body, String... keys) {
        String value = stringValue(body, keys);
        if (value == null || value.isBlank()) {
            throw new BusinessException(400, "required field missing: " + String.join("/", keys));
        }
        return value;
    }

    static Long longValue(Map<String, Object> body, String... keys) {
        String value = stringValue(body, keys);
        return value == null || value.isBlank() ? null : Long.parseLong(value);
    }

    static Long requiredLong(Map<String, Object> body, String... keys) {
        Long value = longValue(body, keys);
        if (value == null) {
            throw new BusinessException(400, "required field missing: " + String.join("/", keys));
        }
        return value;
    }

    static Integer intValue(Map<String, Object> body, Integer defaultValue, String... keys) {
        String value = stringValue(body, keys);
        return value == null || value.isBlank() ? defaultValue : Integer.parseInt(value);
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> mapValue(Map<String, Object> body, String key) {
        Object value = body == null ? null : body.get(key);
        if (!(value instanceof Map<?, ?> map)) return new LinkedHashMap<>();
        Map<String, Object> result = new LinkedHashMap<>();
        map.forEach((k, v) -> result.put(String.valueOf(k), v));
        return result;
    }

    static List<Map<String, Object>> listOfMaps(Map<String, Object> body, String key) {
        Object value = body == null ? null : body.get(key);
        if (!(value instanceof List<?> list)) return new ArrayList<>();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                Map<String, Object> row = new LinkedHashMap<>();
                map.forEach((k, v) -> row.put(String.valueOf(k), v));
                result.add(row);
            }
        }
        return result;
    }

    static LocalDateTime dateTime(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return LocalDateTime.parse(value);
        } catch (DateTimeParseException ignored) {
            try {
                return OffsetDateTime.parse(value).toLocalDateTime();
            } catch (DateTimeParseException ex) {
                throw new BusinessException(400, "invalid ISO-8601 date time: " + value);
            }
        }
    }
}
