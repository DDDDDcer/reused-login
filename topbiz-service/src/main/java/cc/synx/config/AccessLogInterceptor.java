package cc.synx.config;

import cc.synx.api.LogApiClient;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class AccessLogInterceptor implements HandlerInterceptor {
    private static final String START_TIME_ATTR = "topbizAccessLogStart";
    private static final String TRACE_ID_ATTR = "topbizTraceId";

    private final LogApiClient logApiClient;

    public AccessLogInterceptor(LogApiClient logApiClient) {
        this.logApiClient = logApiClient;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String traceId = request.getHeader("X-Trace-Id");
        if (traceId == null || traceId.isBlank()) {
            traceId = "trace-" + UUID.randomUUID();
        }
        request.setAttribute(START_TIME_ATTR, System.currentTimeMillis());
        request.setAttribute(TRACE_ID_ATTR, traceId);
        response.setHeader("X-Trace-Id", traceId);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        Long start = (Long) request.getAttribute(START_TIME_ATTR);
        long costMs = start == null ? 0 : System.currentTimeMillis() - start;
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("request_time", LocalDateTime.now().toString());
        body.put("user_id", headerOrDefault(request, "X-User-Id", "anonymous"));
        body.put("client_ip", clientIp(request));
        body.put("service_name", "topbiz");
        body.put("path", request.getRequestURI());
        body.put("method", request.getMethod());
        body.put("status_code", response.getStatus());
        body.put("cost_ms", costMs);
        body.put("trace_id", request.getAttribute(TRACE_ID_ATTR));
        body.put("message", ex == null ? "TopBiz access" : ex.getMessage());
        try {
            logApiClient.reportAccess(body);
        } catch (Exception ignored) {
            // Access logging must not break the business response.
        }
    }

    private String headerOrDefault(HttpServletRequest request, String header, String fallback) {
        String value = request.getHeader(header);
        return value == null || value.isBlank() ? fallback : value;
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
