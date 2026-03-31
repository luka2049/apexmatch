package com.apexmatch.fundchain.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 资金流水记录 - 追踪所有资金变动
 */
@Data
public class FundFlow {
    private Long flowId;
    private Long userId;
    private String currencyCode;
    private String chainCode;
    private String flowType;
    private BigDecimal amount;
    private BigDecimal balanceBefore;
    private BigDecimal balanceAfter;
    private String refType;
    private String refId;
    private String txHash;
    private String remark;
    private LocalDateTime createdAt;
}
