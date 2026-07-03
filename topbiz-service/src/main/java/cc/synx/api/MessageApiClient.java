package cc.synx.api;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.DeleteExchange;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import java.util.Map;

@HttpExchange("/api/v1/messages")
public interface MessageApiClient {
    @PostExchange("/send-now")
    Map<String, Object> sendNow(@RequestHeader(name = "X-Sender-Id", required = false) String senderId,
                                @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
                                @RequestBody Map<String, Object> body);

    @PostExchange("/send-scheduled")
    Map<String, Object> sendScheduled(@RequestHeader(name = "X-Sender-Id", required = false) String senderId,
                                      @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
                                      @RequestBody Map<String, Object> body);

    @GetExchange("/tasks/{taskId}")
    Map<String, Object> taskDetail(@PathVariable Long taskId);

    @GetExchange("/records")
    Map<String, Object> records(@RequestHeader(name = "X-Sender-Id", required = false) String senderId,
                                @RequestParam(required = false) String status,
                                @RequestParam(name = "carrier_type", required = false) String carrierType,
                                @RequestParam(name = "receiver_id", required = false) String receiverId,
                                @RequestParam(name = "start_time", required = false) String startTime,
                                @RequestParam(name = "end_time", required = false) String endTime,
                                @RequestParam(defaultValue = "1") int page,
                                @RequestParam(name = "page_size", defaultValue = "20") int pageSize);

    @DeleteExchange("/local/{messageId}")
    Map<String, Object> deleteLocal(@RequestHeader(name = "X-User-Id", required = false) String userId,
                                    @PathVariable Long messageId);
}
