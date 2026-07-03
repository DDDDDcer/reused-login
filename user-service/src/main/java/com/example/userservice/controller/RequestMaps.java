package com.example.userservice.controller;

import java.util.LinkedHashMap;
import java.util.Map;

final class RequestMaps {
    private RequestMaps() {
    }

    static String stringValue(Map<String, Object> body, String... keys) {
        if (body == null) {
            return null;
        }
        for (String key : keys) {
            Object value = body.get(key);
            if (value != null) {
                return String.valueOf(value);
            }
        }
        return null;
    }

    static Long longValue(Map<String, Object> body, String... keys) {
        String value = stringValue(body, keys);
        if (value == null || value.isBlank()) {
            return null;
        }
        return Long.parseLong(value);
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> mapValue(Map<String, Object> body, String key) {
        if (body == null || body.get(key) == null) {
            return new LinkedHashMap<>();
        }
        Object value = body.get(key);
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            map.forEach((k, v) -> result.put(String.valueOf(k), v));
            return result;
        }
        return new LinkedHashMap<>();
    }
}
