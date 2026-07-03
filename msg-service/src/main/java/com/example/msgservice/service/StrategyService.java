package com.example.msgservice.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.msgservice.common.BusinessException;
import com.example.msgservice.entity.MessageStrategy;
import com.example.msgservice.mapper.MessageStrategyMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class StrategyService {
    private final MessageStrategyMapper strategyMapper;

    public StrategyService(MessageStrategyMapper strategyMapper) {
        this.strategyMapper = strategyMapper;
    }

    public Map<String, Object> list(String status) {
        LambdaQueryWrapper<MessageStrategy> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) {
            wrapper.eq(MessageStrategy::getStatus, status.toUpperCase());
        }
        List<MessageStrategy> strategies = strategyMapper.selectList(wrapper.orderByAsc(MessageStrategy::getId));
        return Map.of("strategy_list", strategies, "total", strategies.size());
    }

    public Map<String, Object> create(String name, Integer maxRetries, Integer retryIntervalSeconds, String status) {
        if (strategyMapper.selectCount(new LambdaQueryWrapper<MessageStrategy>()
                .eq(MessageStrategy::getStrategyName, name)) > 0) {
            throw new BusinessException(409, "strategy_name already exists");
        }
        MessageStrategy strategy = new MessageStrategy();
        strategy.setStrategyName(name);
        strategy.setMaxRetries(maxRetries == null ? 0 : maxRetries);
        strategy.setRetryIntervalSeconds(retryIntervalSeconds == null ? 60 : retryIntervalSeconds);
        strategy.setStatus(status == null || status.isBlank() ? "ENABLED" : status.toUpperCase());
        strategyMapper.insert(strategy);
        return Map.of("strategy_id", strategy.getId());
    }

    public Map<String, Object> update(Long strategyId, String name, Integer maxRetries,
                                      Integer retryIntervalSeconds, String status) {
        MessageStrategy strategy = strategyMapper.selectById(strategyId);
        if (strategy == null) throw new BusinessException(404, "strategy not found: " + strategyId);
        if (name != null && !name.isBlank()) strategy.setStrategyName(name);
        if (maxRetries != null) strategy.setMaxRetries(maxRetries);
        if (retryIntervalSeconds != null) strategy.setRetryIntervalSeconds(retryIntervalSeconds);
        if (status != null && !status.isBlank()) strategy.setStatus(status.toUpperCase());
        strategyMapper.updateById(strategy);
        return Map.of("updated", true);
    }
}
