package com.apexmatch.common.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;

/**
 * 成交事件
 * 记录每笔成交的详细信息
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TradeEvent extends DomainEvent {

    /**
     * 成交ID
     */
    private Long tradeId;

    /**
     * 交易对
     */
    private String symbol;

    /**
     * 成交价格
     */
    private BigDecimal price;

    /**
     * 成交数量
     */
    private BigDecimal quantity;

    /**
     * Taker 订单ID
     */
    private Long takerOrderId;

    /**
     * Maker 订单ID
     */
    private Long makerOrderId;

    /**
     * Taker 用户ID
     */
    private Long takerUserId;

    /**
     * Maker 用户ID
     */
    private Long makerUserId;

    /**
     * 成交时间戳
     */
    private Long tradeTime;

    /**
     * Taker 手续费
     */
    private BigDecimal takerFee;

    /**
     * Maker 手续费
     */
    private BigDecimal makerFee;

    public TradeEvent() {
        super();
        setEventType("TRADE_EVENT");
        setSource("apexmatch-engine");
    }
}

