package cc.synx.api;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import java.util.Map;

@HttpExchange
public interface LogApiClient {
    @PostExchange("/internal/logs/access")
    Map<String, Object> reportAccess(@RequestBody Map<String, Object> body);

    @PostExchange("/api/v1/log/search")
    Map<String, Object> search(@RequestBody Map<String, Object> body);

    @GetExchange("/api/v1/log/health")
    Map<String, Object> health();
}
