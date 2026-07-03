package com.example.msgservice.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.msgservice.common.BusinessException;
import com.example.msgservice.entity.CarrierAccount;
import com.example.msgservice.entity.MessageCarrier;
import com.example.msgservice.mapper.CarrierAccountMapper;
import com.example.msgservice.mapper.MessageCarrierMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CarrierService {
    private final MessageCarrierMapper carrierMapper;
    private final CarrierAccountMapper accountMapper;
    private final ObjectMapper objectMapper;

    public CarrierService(MessageCarrierMapper carrierMapper, CarrierAccountMapper accountMapper, ObjectMapper objectMapper) {
        this.carrierMapper = carrierMapper;
        this.accountMapper = accountMapper;
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> createCarrier(String type, String name, String description, String status) {
        String normalizedType = type.toUpperCase();
        if (carrierMapper.selectCount(new LambdaQueryWrapper<MessageCarrier>()
                .eq(MessageCarrier::getCarrierType, normalizedType)) > 0) {
            throw new BusinessException(409, "carrier_type already exists");
        }
        MessageCarrier carrier = new MessageCarrier();
        carrier.setCarrierType(normalizedType);
        carrier.setCarrierName(name);
        carrier.setDescription(description);
        carrier.setStatus(status == null ? "ENABLED" : status.toUpperCase());
        carrierMapper.insert(carrier);
        return Map.of("carrier_id", carrier.getId());
    }

    public Map<String, Object> updateCarrier(Long id, String name, String description, String status) {
        MessageCarrier carrier = requireCarrier(id);
        if (name != null) carrier.setCarrierName(name);
        if (description != null) carrier.setDescription(description);
        if (status != null) carrier.setStatus(status.toUpperCase());
        carrierMapper.updateById(carrier);
        return Map.of("updated", true);
    }

    public Map<String, Object> list(String type, String status) {
        LambdaQueryWrapper<MessageCarrier> wrapper = new LambdaQueryWrapper<>();
        if (type != null && !type.isBlank()) wrapper.eq(MessageCarrier::getCarrierType, type.toUpperCase());
        if (status != null && !status.isBlank()) wrapper.eq(MessageCarrier::getStatus, status.toUpperCase());
        List<MessageCarrier> carriers = carrierMapper.selectList(wrapper.orderByAsc(MessageCarrier::getId));
        return Map.of("carrier_list", carriers, "total", carriers.size());
    }

    public Map<String, Object> createAccount(Long carrierId, String name, String provider, String accessKey,
                                              Map<String, Object> config, String status) {
        requireCarrier(carrierId);
        CarrierAccount account = new CarrierAccount();
        account.setCarrierId(carrierId);
        account.setAccountName(name);
        account.setProvider(provider);
        account.setAccessKey(accessKey);
        account.setConfigJson(toJson(config));
        account.setStatus(status == null ? "ENABLED" : status.toUpperCase());
        accountMapper.insert(account);
        return Map.of("account_id", account.getId());
    }

    public Map<String, Object> updateAccountStatus(Long accountId, String status) {
        CarrierAccount account = accountMapper.selectById(accountId);
        if (account == null) throw new BusinessException(404, "carrier account not found: " + accountId);
        account.setStatus(status.toUpperCase());
        accountMapper.updateById(account);
        return Map.of("updated", true);
    }

    public CarrierSelection selectEnabled(String type) {
        MessageCarrier carrier = carrierMapper.selectOne(new LambdaQueryWrapper<MessageCarrier>()
                .eq(MessageCarrier::getCarrierType, type.toUpperCase())
                .eq(MessageCarrier::getStatus, "ENABLED")
                .last("LIMIT 1"));
        if (carrier == null) throw new BusinessException(400, "enabled carrier not found: " + type);
        CarrierAccount account = accountMapper.selectOne(new LambdaQueryWrapper<CarrierAccount>()
                .eq(CarrierAccount::getCarrierId, carrier.getId())
                .eq(CarrierAccount::getStatus, "ENABLED")
                .orderByAsc(CarrierAccount::getId)
                .last("LIMIT 1"));
        if (account == null) throw new BusinessException(400, "enabled carrier account not found: " + type);
        return new CarrierSelection(carrier, account);
    }

    private MessageCarrier requireCarrier(Long id) {
        MessageCarrier carrier = carrierMapper.selectById(id);
        if (carrier == null) throw new BusinessException(404, "carrier not found: " + id);
        return carrier;
    }

    private String toJson(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(400, "invalid config");
        }
    }

    public record CarrierSelection(MessageCarrier carrier, CarrierAccount account) {
    }
}
