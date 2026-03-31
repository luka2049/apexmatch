package com.apexmatch.risk.monitor;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 仓位限额控制服务
 * 控制单用户单合约持仓上限、全平台总持仓上限，避免风险集中
 */
@Slf4j
@Service
public class PositionLimitController {

    // 单用户单合约持仓上限（BTC 数量）
    private static final BigDecimal USER_POSITION_LIMIT = new BigDecimal("100");

    // 全平台单合约总持仓上限（BTC 数量）
    private static final BigDecimal PLATFORM_POSITION_LIMIT = new BigDecimal("10000");

    private final Map<String, UserPositionInfo> userPositions = new ConcurrentHashMap<>();
    private final Map<String, PlatformPositionInfo> platformPositions = new ConcurrentHashMap<>();

    /**
     * 检查用户仓位限额
     */
    public PositionLimitResult checkUserLimit(Long userId, String symbol, BigDecimal quantity) {
        String key = userId + "_" + symbol;
        UserPositionInfo info = userPositions.computeIfAbsent(key, k -> {
            UserPositionInfo newInfo = new UserPositionInfo();
            newInfo.setUserId(userId);
            newInfo.setSymbol(symbol);
            newInfo.setCurrentPosition(BigDecimal.ZERO);
            return newInfo;
        });

        BigDecimal newPosition = info.getCurrentPosition().add(quantity);

        if (newPosition.abs().compareTo(USER_POSITION_LIMIT) > 0) {
            log.warn("用户仓位超限: userId={}, symbol={}, current={}, new={}, limit={}",
                    userId, symbol, info.getCurrentPosition(), newPosition, USER_POSITION_LIMIT);
            return PositionLimitResult.reject("用户仓位超限");
        }

        return PositionLimitResult.allow();
    }

    /**
     * 检查平台仓位限额
     */
    public PositionLimitResult checkPlatformLimit(String symbol, BigDecimal quantity) {
        PlatformPositionInfo info = platformPositions.computeIfAbsent(symbol, k -> {
            PlatformPositionInfo newInfo = new PlatformPositionInfo();
            newInfo.setSymbol(symbol);
            newInfo.setTotalLongPosition(BigDecimal.ZERO);
            newInfo.setTotalShortPosition(BigDecimal.ZERO);
            return newInfo;
        });

        BigDecimal newTotal;
        if (quantity.compareTo(BigDecimal.ZERO) > 0) {
            newTotal = info.getTotalLongPosition().add(quantity);
        } else {
            newTotal = info.getTotalShortPosition().add(quantity.abs());
        }

        if (newTotal.compareTo(PLATFORM_POSITION_LIMIT) > 0) {
            log.warn("平台仓位超限: symbol={}, side={}, current={}, new={}, limit={}",
                    symbol, quantity.compareTo(BigDecimal.ZERO) > 0 ? "LONG" : "SHORT",
                    quantity.compareTo(BigDecimal.ZERO) > 0 ? info.getTotalLongPosition() : info.getTotalShortPosition(),
                    newTotal, PLATFORM_POSITION_LIMIT);
            return PositionLimitResult.reject("平台仓位超限");
        }

        return PositionLimitResult.allow();
    }

    /**
     * 更新用户仓位
     */
    public void updateUserPosition(Long userId, String symbol, BigDecimal quantity) {
        String key = userId + "_" + symbol;
        UserPositionInfo info = userPositions.computeIfAbsent(key, k -> {
            UserPositionInfo newInfo = new UserPositionInfo();
            newInfo.setUserId(userId);
            newInfo.setSymbol(symbol);
            newInfo.setCurrentPosition(BigDecimal.ZERO);
            return newInfo;
        });

        info.setCurrentPosition(info.getCurrentPosition().add(quantity));
        info.setUpdateTime(LocalDateTime.now());
    }

    /**
     * 更新平台仓位
     */
    public void updatePlatformPosition(String symbol, BigDecimal quantity) {
        PlatformPositionInfo info = platformPositions.computeIfAbsent(symbol, k -> {
            PlatformPositionInfo newInfo = new PlatformPositionInfo();
            newInfo.setSymbol(symbol);
            newInfo.setTotalLongPosition(BigDecimal.ZERO);
            newInfo.setTotalShortPosition(BigDecimal.ZERO);
            return newInfo;
        });

        if (quantity.compareTo(BigDecimal.ZERO) > 0) {
            info.setTotalLongPosition(info.getTotalLongPosition().add(quantity));
        } else {
            info.setTotalShortPosition(info.getTotalShortPosition().add(quantity.abs()));
        }
        info.setUpdateTime(LocalDateTime.now());
    }

    @Data
    public static class UserPositionInfo {
        private Long userId;
        private String symbol;
        private BigDecimal currentPosition;
        private LocalDateTime updateTime;
    }

    @Data
    public static class PlatformPositionInfo {
        private String symbol;
        private BigDecimal totalLongPosition;
        private BigDecimal totalShortPosition;
        private LocalDateTime updateTime;
    }

    @Data
    public static class PositionLimitResult {
        private boolean allowed;
        private String rejectReason;

        public static PositionLimitResult allow() {
            PositionLimitResult result = new PositionLimitResult();
            result.setAllowed(true);
            return result;
        }

        public static PositionLimitResult reject(String reason) {
            PositionLimitResult result = new PositionLimitResult();
            result.setAllowed(false);
            result.setRejectReason(reason);
            return result;
        }
    }
}
