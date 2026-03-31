package com.apexmatch.risk.monitor;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 异常交易拦截服务
 * 监控高频交易、大额资金划转、异常盈亏，实时告警与拦截
 */
@Slf4j
@Service
public class AbnormalTradeInterceptor {

    // 高频交易阈值：1 分钟内超过 100 笔订单
    private static final int HIGH_FREQUENCY_THRESHOLD = 100;
    private static final long HIGH_FREQUENCY_WINDOW_MS = 60_000;

    // 大额资金划转阈值：单笔超过 100,000 USDT
    private static final BigDecimal LARGE_TRANSFER_THRESHOLD = new BigDecimal("100000");

    // 异常盈亏阈值：单日盈利超过 1,000,000 USDT
    private static final BigDecimal ABNORMAL_PNL_THRESHOLD = new BigDecimal("1000000");

    private final Map<Long, UserTradeActivity> userActivities = new ConcurrentHashMap<>();
    private final List<AbnormalTradeAlert> alerts = new ArrayList<>();

    /**
     * 检测高频交易
     */
    public InterceptResult checkHighFrequency(Long userId) {
        UserTradeActivity activity = userActivities.computeIfAbsent(userId, k -> new UserTradeActivity());

        long now = System.currentTimeMillis();
        activity.getOrderTimestamps().removeIf(ts -> now - ts > HIGH_FREQUENCY_WINDOW_MS);
        activity.getOrderTimestamps().add(now);

        if (activity.getOrderTimestamps().size() > HIGH_FREQUENCY_THRESHOLD) {
            log.warn("高频交易告警: userId={}, count={}, window={}ms",
                    userId, activity.getOrderTimestamps().size(), HIGH_FREQUENCY_WINDOW_MS);

            createAlert(userId, "HIGH_FREQUENCY", "1分钟内下单超过" + HIGH_FREQUENCY_THRESHOLD + "笔");
            return InterceptResult.reject("高频交易拦截");
        }

        return InterceptResult.allow();
    }

    /**
     * 检测大额资金划转
     */
    public InterceptResult checkLargeTransfer(Long userId, BigDecimal amount) {
        if (amount.abs().compareTo(LARGE_TRANSFER_THRESHOLD) > 0) {
            log.warn("大额资金划转告警: userId={}, amount={}", userId, amount);

            createAlert(userId, "LARGE_TRANSFER", "单笔划转金额: " + amount);
            return InterceptResult.manualReview("大额资金划转，需人工审核");
        }

        return InterceptResult.allow();
    }

    /**
     * 检测异常盈亏
     */
    public InterceptResult checkAbnormalPnl(Long userId, BigDecimal dailyPnl) {
        if (dailyPnl.compareTo(ABNORMAL_PNL_THRESHOLD) > 0) {
            log.warn("异常盈亏告警: userId={}, dailyPnl={}", userId, dailyPnl);

            createAlert(userId, "ABNORMAL_PNL", "单日盈利: " + dailyPnl);
            return InterceptResult.manualReview("异常盈亏，需人工审核");
        }

        return InterceptResult.allow();
    }

    /**
     * 创建告警
     */
    private void createAlert(Long userId, String alertType, String description) {
        AbnormalTradeAlert alert = new AbnormalTradeAlert();
        alert.setAlertId(System.currentTimeMillis());
        alert.setUserId(userId);
        alert.setAlertType(alertType);
        alert.setDescription(description);
        alert.setStatus("OPEN");
        alert.setCreateTime(LocalDateTime.now());
        alerts.add(alert);
    }

    /**
     * 获取未处理告警
     */
    public List<AbnormalTradeAlert> getOpenAlerts() {
        return alerts.stream()
                .filter(a -> "OPEN".equals(a.getStatus()))
                .toList();
    }

    @Data
    public static class UserTradeActivity {
        private List<Long> orderTimestamps = new ArrayList<>();
    }

    @Data
    public static class AbnormalTradeAlert {
        private Long alertId;
        private Long userId;
        private String alertType;
        private String description;
        private String status;
        private LocalDateTime createTime;
    }

    @Data
    public static class InterceptResult {
        private boolean allowed;
        private boolean needManualReview;
        private String rejectReason;

        public static InterceptResult allow() {
            InterceptResult result = new InterceptResult();
            result.setAllowed(true);
            result.setNeedManualReview(false);
            return result;
        }

        public static InterceptResult reject(String reason) {
            InterceptResult result = new InterceptResult();
            result.setAllowed(false);
            result.setNeedManualReview(false);
            result.setRejectReason(reason);
            return result;
        }

        public static InterceptResult manualReview(String reason) {
            InterceptResult result = new InterceptResult();
            result.setAllowed(false);
            result.setNeedManualReview(true);
            result.setRejectReason(reason);
            return result;
        }
    }
}
