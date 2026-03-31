package com.apexmatch.common.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;

/**
 * 结算事件
 * 记录资金费率结算、交割结算等
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SettlementEvent extends DomainEvent {

    /**
     * 结算ID
     */
    private Long settlementId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 交易对
     */
    private String symbol;

    /**
     * 结算类型（FUNDING, DELIVERY）
     */
    private String settlementType;

    /**
     * 结算金额
     */
    private BigDecimal amount;

    /**
     * 持仓方向（LONG, SHORT）
     */
    private String positionSide;

    /**
     * 持仓数量
     */
    private BigDecimal positionQuantity;

    /**
     * 结算状态（PENDING, SETTLED, FAILED）
     */
    private String status;

    public SettlementEvent() {
        super();
        setEventType("SETTLEMENT_EVENT");
        setSource("apexmatch-settlement");
    }
}
