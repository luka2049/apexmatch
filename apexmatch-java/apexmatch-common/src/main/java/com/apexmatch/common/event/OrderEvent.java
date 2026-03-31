package com.apexmatch.common.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;

/**
 * 订单事件
 * 记录订单生命周期中的所有状态变更
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OrderEvent extends DomainEvent {

    /**
     * 订单ID
     */
    private Long orderId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 交易对
     */
    private String symbol;

    /**
     * 订单类型（LIMIT, MARKET, STOP）
     */
    private String orderType;

    /**
     * 订单方向（BUY, SELL）
     */
    private String side;

    /**
     * 订单价格
     */
    private BigDecimal price;

    /**
     * 订单数量
     */
    private BigDecimal quantity;

    /**
     * 订单状态（NEW, PARTIALLY_FILLED, FILLED, CANCELLED）
     */
    private String status;

    /**
     * 已成交数量
     */
    private BigDecimal filledQuantity;

    /**
     * 事件动作（CREATED, MATCHED, CANCELLED, EXPIRED）
     */
    private String action;

    /**
     * 客户端订单ID（幂等性）
     */
    private String clientOrderId;

    public OrderEvent() {
        super();
        setEventType("ORDER_EVENT");
        setSource("apexmatch-engine");
    }
}

