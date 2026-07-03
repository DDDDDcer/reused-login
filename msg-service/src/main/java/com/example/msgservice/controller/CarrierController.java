package com.example.msgservice.controller;

import com.example.msgservice.common.ApiResponse;
import com.example.msgservice.service.CarrierService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@Tag(name = "Message Carriers")
public class CarrierController {
    private final CarrierService carrierService;

    public CarrierController(CarrierService carrierService) {
        this.carrierService = carrierService;
    }

    @PostMapping("/api/v1/message-carriers")
    public ApiResponse<Map<String, Object>> createCarrier(@RequestBody Map<String, Object> body) {
        return ApiResponse.ok(carrierService.createCarrier(
                RequestMaps.required(body, "carrier_type", "carrierType"),
                RequestMaps.required(body, "carrier_name", "carrierName"),
                RequestMaps.stringValue(body, "description"),
                RequestMaps.stringValue(body, "status")));
    }

    @PutMapping("/api/v1/message-carriers/{carrierId}")
    public ApiResponse<Map<String, Object>> updateCarrier(@PathVariable Long carrierId,
                                                           @RequestBody Map<String, Object> body) {
        return ApiResponse.ok(carrierService.updateCarrier(
                carrierId,
                RequestMaps.stringValue(body, "carrier_name", "carrierName"),
                RequestMaps.stringValue(body, "description"),
                RequestMaps.stringValue(body, "status")));
    }

    @GetMapping("/api/v1/message-carriers")
    public ApiResponse<Map<String, Object>> listCarriers(
            @RequestParam(name = "carrier_type", required = false) String carrierType,
            @RequestParam(required = false) String status) {
        return ApiResponse.ok(carrierService.list(carrierType, status));
    }

    @PostMapping("/api/v1/message-carriers/{carrierId}/accounts")
    public ApiResponse<Map<String, Object>> createAccount(@PathVariable Long carrierId,
                                                           @RequestBody Map<String, Object> body) {
        return ApiResponse.ok(carrierService.createAccount(
                carrierId,
                RequestMaps.required(body, "account_name", "accountName"),
                RequestMaps.required(body, "provider"),
                RequestMaps.stringValue(body, "access_key", "accessKey"),
                RequestMaps.mapValue(body, "config"),
                RequestMaps.stringValue(body, "status")));
    }

    @PutMapping("/api/v1/carrier-accounts/{accountId}/status")
    public ApiResponse<Map<String, Object>> updateAccountStatus(@PathVariable Long accountId,
                                                                 @RequestBody Map<String, Object> body) {
        return ApiResponse.ok(carrierService.updateAccountStatus(accountId, RequestMaps.required(body, "status")));
    }
}
