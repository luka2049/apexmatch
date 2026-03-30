package com.apexmatch.risk.service;

import java.math.BigDecimal;

/**
 * 止损/止盈监控服务。
 * 监听每笔成交后的最新价格，触发止损/止盈订单。
 *
 * @author luka
 * @since 2025-03-30
 */
public interface SlTpMonitorService {

    /**
     * 检查并触发止损/止盈订单。
     *
     * @param userId    用户ID
     * @param symbol    交易对
     * @param lastPrice 最新成交价
     */
    void checkAndTrigger(long userId, String symbol, BigDecimal lastPrice);
}
