package com.apexmatch.risk.monitor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 异常交易拦截测试
 */
@DisplayName("异常交易拦截测试")
class AbnormalTradeInterceptorTest {

    private AbnormalTradeInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new AbnormalTradeInterceptor();
    }

    @Test
    @DisplayName("高频交易拦截验证")
    void testHighFrequencyInterception() {
        Long userId = 1001L;

        // 1. 正常频率，允许交易
        for (int i = 0; i < 50; i++) {
            AbnormalTradeInterceptor.InterceptResult result =
                    interceptor.checkHighFrequency(userId);
            assertTrue(result.isAllowed(), "正常频率应允许交易");
        }

        // 2. 高频交易，触发拦截
        for (int i = 0; i < 60; i++) {
            interceptor.checkHighFrequency(userId);
        }

        AbnormalTradeInterceptor.InterceptResult result =
                interceptor.checkHighFrequency(userId);

        assertFalse(result.isAllowed(), "高频交易应触发拦截");
        assertEquals("高频交易拦截", result.getRejectReason());

        // 3. 验证告警生成
        List<AbnormalTradeInterceptor.AbnormalTradeAlert> alerts =
                interceptor.getOpenAlerts();

        assertFalse(alerts.isEmpty(), "应生成告警");
        assertEquals("HIGH_FREQUENCY", alerts.get(0).getAlertType());
    }

    @Test
    @DisplayName("大额资金划转拦截验证")
    void testLargeTransferInterception() {
        Long userId = 1001L;

        // 1. 正常金额，允许划转
        BigDecimal normalAmount = new BigDecimal("50000");
        AbnormalTradeInterceptor.InterceptResult normalResult =
                interceptor.checkLargeTransfer(userId, normalAmount);

        assertTrue(normalResult.isAllowed(), "正常金额应允许划转");

        // 2. 大额划转，需人工审核
        BigDecimal largeAmount = new BigDecimal("150000");
        AbnormalTradeInterceptor.InterceptResult largeResult =
                interceptor.checkLargeTransfer(userId, largeAmount);

        assertFalse(largeResult.isAllowed(), "大额划转应拦截");
        assertTrue(largeResult.isNeedManualReview(), "大额划转需人工审核");
        assertEquals("大额资金划转，需人工审核", largeResult.getRejectReason());

        // 3. 验证告警生成
        List<AbnormalTradeInterceptor.AbnormalTradeAlert> alerts =
                interceptor.getOpenAlerts();

        assertFalse(alerts.isEmpty(), "应生成告警");
        assertEquals("LARGE_TRANSFER", alerts.get(0).getAlertType());
    }

    @Test
    @DisplayName("异常盈亏拦截验证")
    void testAbnormalPnlInterception() {
        Long userId = 1001L;

        // 1. 正常盈亏，允许
        BigDecimal normalPnl = new BigDecimal("50000");
        AbnormalTradeInterceptor.InterceptResult normalResult =
                interceptor.checkAbnormalPnl(userId, normalPnl);

        assertTrue(normalResult.isAllowed(), "正常盈亏应允许");

        // 2. 异常盈亏，需人工审核
        BigDecimal abnormalPnl = new BigDecimal("1500000");
        AbnormalTradeInterceptor.InterceptResult abnormalResult =
                interceptor.checkAbnormalPnl(userId, abnormalPnl);

        assertFalse(abnormalResult.isAllowed(), "异常盈亏应拦截");
        assertTrue(abnormalResult.isNeedManualReview(), "异常盈亏需人工审核");

        // 3. 验证告警生成
        List<AbnormalTradeInterceptor.AbnormalTradeAlert> alerts =
                interceptor.getOpenAlerts();

        assertFalse(alerts.isEmpty(), "应生成告警");
        assertEquals("ABNORMAL_PNL", alerts.get(alerts.size() - 1).getAlertType());
    }
}
