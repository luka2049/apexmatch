package com.apexmatch.risk.monitor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 价格异常检测测试
 * 对应测试用例：RC-006
 */
@DisplayName("价格异常检测测试")
class PriceAnomalyDetectorTest {

    private PriceAnomalyDetector detector;

    @BeforeEach
    void setUp() {
        detector = new PriceAnomalyDetector();
    }

    @Test
    @DisplayName("RC-006: 价格异常强平暂停验证")
    void testPriceAnomalyLiquidationSuspension() {
        String symbol = "BTC-USDT";
        BigDecimal spotPrice = new BigDecimal("50000");

        // 1. 正常价格，允许强平
        BigDecimal normalMarkPrice = new BigDecimal("50500");
        PriceAnomalyDetector.PriceAnomalyResult normalResult =
                detector.detectAnomaly(symbol, normalMarkPrice, spotPrice);

        assertFalse(normalResult.isAbnormal(), "正常价格不应触发异常");
        assertTrue(detector.isLiquidationAllowed(symbol), "正常价格应允许强平");

        // 2. 价格异常（偏差超过 20%），暂停强平
        BigDecimal abnormalMarkPrice = new BigDecimal("65000");
        PriceAnomalyDetector.PriceAnomalyResult abnormalResult =
                detector.detectAnomaly(symbol, abnormalMarkPrice, spotPrice);

        assertTrue(abnormalResult.isAbnormal(), "价格偏差超过 20% 应触发异常");
        assertFalse(detector.isLiquidationAllowed(symbol), "价格异常期间应暂停强平");

        // 3. 价格恢复正常（偏差小于 5%），恢复强平
        BigDecimal recoveryMarkPrice = new BigDecimal("50100");
        PriceAnomalyDetector.PriceAnomalyResult recoveryResult =
                detector.detectAnomaly(symbol, recoveryMarkPrice, spotPrice);

        assertFalse(recoveryResult.isAbnormal(), "价格恢复正常不应触发异常");
        assertTrue(detector.isLiquidationAllowed(symbol), "价格恢复后应允许强平");
    }

    @Test
    @DisplayName("价格偏差计算准确性验证")
    void testDeviationCalculation() {
        String symbol = "BTC-USDT";
        BigDecimal spotPrice = new BigDecimal("50000");
        BigDecimal markPrice = new BigDecimal("60000");

        detector.detectAnomaly(symbol, markPrice, spotPrice);

        PriceAnomalyDetector.PriceStatus status = detector.getPriceStatus(symbol);
        assertNotNull(status);

        // 偏差率 = |60000 - 50000| / 50000 = 0.2 = 20%
        BigDecimal expectedDeviation = new BigDecimal("0.20000000");
        assertEquals(0, expectedDeviation.compareTo(status.getDeviation()),
                "偏差率计算应准确");
    }

    @Test
    @DisplayName("边界值测试：恰好达到阈值")
    void testThresholdBoundary() {
        String symbol = "BTC-USDT";
        BigDecimal spotPrice = new BigDecimal("50000");

        // 恰好 20% 偏差
        BigDecimal boundaryMarkPrice = new BigDecimal("60000");
        PriceAnomalyDetector.PriceAnomalyResult result =
                detector.detectAnomaly(symbol, boundaryMarkPrice, spotPrice);

        assertFalse(result.isAbnormal(), "恰好达到阈值不应触发异常（使用 > 判断）");
    }
}
