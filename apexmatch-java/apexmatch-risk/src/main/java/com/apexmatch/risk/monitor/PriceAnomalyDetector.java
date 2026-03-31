package com.apexmatch.risk.monitor;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 价格异常检测服务
 * 监控标记价格与现货价格偏差，避免价格操纵导致的恶意爆仓
 */
@Slf4j
@Service
public class PriceAnomalyDetector {

    // 价格异常阈值：20%
    private static final BigDecimal ANOMALY_THRESHOLD = new BigDecimal("0.20");

    // 价格恢复阈值：5%
    private static final BigDecimal RECOVERY_THRESHOLD = new BigDecimal("0.05");

    private final Map<String, PriceStatus> priceStatusMap = new ConcurrentHashMap<>();

    /**
     * 检测价格异常
     * 标记价格与现货指数价格偏差超过阈值时，标记为异常
     */
    public PriceAnomalyResult detectAnomaly(String symbol, BigDecimal markPrice, BigDecimal spotPrice) {
        if (markPrice == null || spotPrice == null || spotPrice.compareTo(BigDecimal.ZERO) == 0) {
            return PriceAnomalyResult.normal(symbol);
        }

        // 计算偏差率 = |标记价格 - 现货价格| / 现货价格
        BigDecimal deviation = markPrice.subtract(spotPrice)
                .abs()
                .divide(spotPrice, 8, RoundingMode.HALF_UP);

        PriceStatus status = priceStatusMap.computeIfAbsent(symbol, k -> new PriceStatus());
        status.setSymbol(symbol);
        status.setMarkPrice(markPrice);
        status.setSpotPrice(spotPrice);
        status.setDeviation(deviation);
        status.setUpdateTime(LocalDateTime.now());

        // 判断是否异常
        if (deviation.compareTo(ANOMALY_THRESHOLD) > 0) {
            if (!status.isAbnormal()) {
                status.setAbnormal(true);
                status.setAbnormalStartTime(LocalDateTime.now());
                log.warn("价格异常告警: symbol={}, markPrice={}, spotPrice={}, deviation={}%",
                        symbol, markPrice, spotPrice, deviation.multiply(new BigDecimal("100")));
            }
            return PriceAnomalyResult.abnormal(symbol, deviation, "价格偏差超过阈值");
        }

        // 判断是否恢复正常
        if (status.isAbnormal() && deviation.compareTo(RECOVERY_THRESHOLD) < 0) {
            status.setAbnormal(false);
            status.setAbnormalEndTime(LocalDateTime.now());
            log.info("价格恢复正常: symbol={}, markPrice={}, spotPrice={}, deviation={}%",
                    symbol, markPrice, spotPrice, deviation.multiply(new BigDecimal("100")));
        }

        return PriceAnomalyResult.normal(symbol);
    }

    /**
     * 获取价格状态
     */
    public PriceStatus getPriceStatus(String symbol) {
        return priceStatusMap.get(symbol);
    }

    /**
     * 是否允许强平
     * 价格异常期间，暂停强平操作
     */
    public boolean isLiquidationAllowed(String symbol) {
        PriceStatus status = priceStatusMap.get(symbol);
        if (status == null) {
            return true;
        }
        return !status.isAbnormal();
    }

    @Data
    public static class PriceStatus {
        private String symbol;
        private BigDecimal markPrice;
        private BigDecimal spotPrice;
        private BigDecimal deviation;
        private boolean abnormal;
        private LocalDateTime abnormalStartTime;
        private LocalDateTime abnormalEndTime;
        private LocalDateTime updateTime;
    }

    @Data
    public static class PriceAnomalyResult {
        private String symbol;
        private boolean abnormal;
        private BigDecimal deviation;
        private String message;

        public static PriceAnomalyResult normal(String symbol) {
            PriceAnomalyResult result = new PriceAnomalyResult();
            result.setSymbol(symbol);
            result.setAbnormal(false);
            return result;
        }

        public static PriceAnomalyResult abnormal(String symbol, BigDecimal deviation, String message) {
            PriceAnomalyResult result = new PriceAnomalyResult();
            result.setSymbol(symbol);
            result.setAbnormal(true);
            result.setDeviation(deviation);
            result.setMessage(message);
            return result;
        }
    }
}
