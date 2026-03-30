package com.apexmatch.gateway.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 撮合引擎核心指标导出。
 * <p>
 * 暴露给 Prometheus 的核心指标：
 * - 撮合 TPS（每秒成交笔数）
 * - 撮合延迟（P50/P95/P99）
 * - Disruptor 队列深度
 * - 订单簿深度
 * - 限流拒绝次数
 * - 熔断触发次数
 * </p>
 *
 * @author luka
 * @since 2025-03-30
 */
@Slf4j
@Component
public class MatchingMetrics {

    private final Counter orderSubmittedCounter;
    private final Counter orderMatchedCounter;
    private final Counter orderCancelledCounter;
    private final Counter rateLimitRejectedCounter;
    private final Counter circuitBreakerTriggeredCounter;
    private final Timer matchingLatencyTimer;
    private final AtomicLong disruptorQueueDepth = new AtomicLong(0);
    private final AtomicLong orderBookDepth = new AtomicLong(0);

    public MatchingMetrics(MeterRegistry registry) {
        // 订单提交计数
        this.orderSubmittedCounter = Counter.builder("apexmatch.order.submitted")
                .description("订单提交总数")
                .register(registry);

        // 订单成交计数
        this.orderMatchedCounter = Counter.builder("apexmatch.order.matched")
                .description("订单成交总数")
                .register(registry);

        // 订单撤销计数
        this.orderCancelledCounter = Counter.builder("apexmatch.order.cancelled")
                .description("订单撤销总数")
                .register(registry);

        // 限流拒绝计数
        this.rateLimitRejectedCounter = Counter.builder("apexmatch.ratelimit.rejected")
                .description("限流拒绝请求数")
                .register(registry);

        // 熔断触发计数
        this.circuitBreakerTriggeredCounter = Counter.builder("apexmatch.circuitbreaker.triggered")
                .description("熔断触发次数")
                .register(registry);

        // 撮合延迟
        this.matchingLatencyTimer = Timer.builder("apexmatch.matching.latency")
                .description("撮合延迟（毫秒）")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry);

        // Disruptor 队列深度
        Gauge.builder("apexmatch.disruptor.queue.depth", disruptorQueueDepth, AtomicLong::get)
                .description("Disruptor 队列深度")
                .register(registry);

        // 订单簿深度
        Gauge.builder("apexmatch.orderbook.depth", orderBookDepth, AtomicLong::get)
                .description("订单簿总挂单数")
                .register(registry);

        log.info("Prometheus Metrics 初始化完成");
    }

    public void recordOrderSubmitted() {
        orderSubmittedCounter.increment();
    }

    public void recordOrderMatched() {
        orderMatchedCounter.increment();
    }

    public void recordOrderCancelled() {
        orderCancelledCounter.increment();
    }

    public void recordRateLimitRejected() {
        rateLimitRejectedCounter.increment();
    }

    public void recordCircuitBreakerTriggered() {
        circuitBreakerTriggeredCounter.increment();
    }

    public void recordMatchingLatency(long latencyMs) {
        matchingLatencyTimer.record(latencyMs, java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    public void updateDisruptorQueueDepth(long depth) {
        disruptorQueueDepth.set(depth);
    }

    public void updateOrderBookDepth(long depth) {
        orderBookDepth.set(depth);
    }
}
