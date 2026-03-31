package com.apexmatch.risk.monitor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 仓位限额控制测试
 */
@DisplayName("仓位限额控制测试")
class PositionLimitControllerTest {

    private PositionLimitController controller;

    @BeforeEach
    void setUp() {
        controller = new PositionLimitController();
    }

    @Test
    @DisplayName("用户仓位限额验证")
    void testUserPositionLimit() {
        Long userId = 1001L;
        String symbol = "BTC-USDT";

        // 1. 正常仓位，允许开仓
        BigDecimal normalQuantity = new BigDecimal("50");
        PositionLimitController.PositionLimitResult normalResult =
                controller.checkUserLimit(userId, symbol, normalQuantity);

        assertTrue(normalResult.isAllowed(), "正常仓位应允许开仓");

        // 2. 更新仓位
        controller.updateUserPosition(userId, symbol, normalQuantity);

        // 3. 超限仓位，拒绝开仓
        BigDecimal excessQuantity = new BigDecimal("60");
        PositionLimitController.PositionLimitResult excessResult =
                controller.checkUserLimit(userId, symbol, excessQuantity);

        assertFalse(excessResult.isAllowed(), "超限仓位应拒绝开仓");
        assertEquals("用户仓位超限", excessResult.getRejectReason());
    }

    @Test
    @DisplayName("平台仓位限额验证")
    void testPlatformPositionLimit() {
        String symbol = "BTC-USDT";

        // 1. 正常仓位，允许开仓
        BigDecimal normalQuantity = new BigDecimal("5000");
        PositionLimitController.PositionLimitResult normalResult =
                controller.checkPlatformLimit(symbol, normalQuantity);

        assertTrue(normalResult.isAllowed(), "正常仓位应允许开仓");

        // 2. 更新平台仓位
        controller.updatePlatformPosition(symbol, normalQuantity);

        // 3. 超限仓位，拒绝开仓
        BigDecimal excessQuantity = new BigDecimal("6000");
        PositionLimitController.PositionLimitResult excessResult =
                controller.checkPlatformLimit(symbol, excessQuantity);

        assertFalse(excessResult.isAllowed(), "超限仓位应拒绝开仓");
        assertEquals("平台仓位超限", excessResult.getRejectReason());
    }

    @Test
    @DisplayName("多空仓位分别统计")
    void testLongShortSeparateLimit() {
        String symbol = "BTC-USDT";

        // 多头仓位
        BigDecimal longQuantity = new BigDecimal("5000");
        controller.updatePlatformPosition(symbol, longQuantity);

        // 空头仓位（负数）
        BigDecimal shortQuantity = new BigDecimal("-5000");
        controller.updatePlatformPosition(symbol, shortQuantity);

        // 验证多空分别统计
        PositionLimitController.PlatformPositionInfo info =
                controller.getPlatformPositionInfo(symbol);

        assertNotNull(info);
        assertEquals(0, longQuantity.compareTo(info.getTotalLongPosition()));
        assertEquals(0, shortQuantity.abs().compareTo(info.getTotalShortPosition()));
    }

    // 辅助方法
    private PositionLimitController.PlatformPositionInfo getPlatformPositionInfo(String symbol) {
        return controller.getPlatformPositionInfo(symbol);
    }
}
