package com.apexmatch.risk.service.impl;

import com.apexmatch.risk.service.InsuranceFundService;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 保险基金服务实现（内存版本）。
 * 生产环境应持久化到数据库。
 *
 * @author luka
 * @since 2025-03-30
 */
@Slf4j
public class InsuranceFundServiceImpl implements InsuranceFundService {

    private final ConcurrentHashMap<String, BigDecimal> balances = new ConcurrentHashMap<>();

    @Override
    public BigDecimal getBalance(String currency) {
        return balances.getOrDefault(currency, BigDecimal.ZERO);
    }

    @Override
    public synchronized void collectLiquidationFee(String currency, BigDecimal amount, long userId, String symbol) {
        BigDecimal current = getBalance(currency);
        BigDecimal newBalance = current.add(amount);
        balances.put(currency, newBalance);
        log.info("保险基金收取强平费 currency={} amount={} userId={} symbol={} newBalance={}",
                currency, amount, userId, symbol, newBalance);
    }

    @Override
    public synchronized void coverLoss(String currency, BigDecimal amount, long userId, String symbol) {
        BigDecimal current = getBalance(currency);
        BigDecimal newBalance = current.subtract(amount);
        balances.put(currency, newBalance);
        log.warn("保险基金兜底亏损 currency={} amount={} userId={} symbol={} newBalance={}",
                currency, amount, userId, symbol, newBalance);

        if (newBalance.signum() < 0) {
            log.error("保险基金不足！currency={} balance={}", currency, newBalance);
            // 生产环境应触发社会化分摊或暂停交易
        }
    }
}
