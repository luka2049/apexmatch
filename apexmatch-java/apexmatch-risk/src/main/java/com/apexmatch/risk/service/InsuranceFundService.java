package com.apexmatch.risk.service;

import java.math.BigDecimal;

/**
 * 保险基金服务。
 * 用于收取强平费用、兜底亏损超出保证金的情况。
 *
 * @author luka
 * @since 2025-03-30
 */
public interface InsuranceFundService {

    /**
     * 获取保险基金余额
     */
    BigDecimal getBalance(String currency);

    /**
     * 收取强平费用（增加保险基金）
     */
    void collectLiquidationFee(String currency, BigDecimal amount, long userId, String symbol);

    /**
     * 兜底亏损（减少保险基金）
     */
    void coverLoss(String currency, BigDecimal amount, long userId, String symbol);
}
