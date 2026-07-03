package cc.synx.api;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;
import org.springframework.web.service.annotation.PutExchange;

import java.util.Map;

@HttpExchange("/api/v1")
public interface MessageAdminApiClient {
    @GetExchange("/message-templates")
    Map<String, Object> listTemplates(@RequestParam(name = "carrier_type", required = false) String carrierType,
                                      @RequestParam(required = false) String status);

    @PostExchange("/message-templates")
    Map<String, Object> createTemplate(@RequestBody Map<String, Object> body);

    @GetExchange("/message-templates/{templateId}")
    Map<String, Object> templateDetail(@PathVariable Long templateId);

    @GetExchange("/message-carriers")
    Map<String, Object> listCarriers(@RequestParam(name = "carrier_type", required = false) String carrierType,
                                     @RequestParam(required = false) String status);

    @PostExchange("/message-carriers")
    Map<String, Object> createCarrier(@RequestBody Map<String, Object> body);

    @PostExchange("/message-carriers/{carrierId}/accounts")
    Map<String, Object> createCarrierAccount(@PathVariable Long carrierId, @RequestBody Map<String, Object> body);

    @PostExchange("/message-receipts")
    Map<String, Object> processReceipt(@RequestBody Map<String, Object> body);

    @GetExchange("/message-strategies")
    Map<String, Object> listStrategies(@RequestParam(required = false) String status);

    @PostExchange("/message-strategies")
    Map<String, Object> createStrategy(@RequestBody Map<String, Object> body);

    @PutExchange("/message-strategies/{strategyId}")
    Map<String, Object> updateStrategy(@PathVariable Long strategyId, @RequestBody Map<String, Object> body);
}
