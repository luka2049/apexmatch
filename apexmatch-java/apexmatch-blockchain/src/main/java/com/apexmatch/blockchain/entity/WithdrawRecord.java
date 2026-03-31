package com.apexmatch.blockchain.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class WithdrawRecord {
    private Long withdrawId;
    private Long userId;
    private String currencyCode;
    private String chainCode;
    private String toAddress;
    private BigDecimal amount;
    private BigDecimal fee;
    private BigDecimal actualAmount;
    private String txHash;
    private String status;
    private String auditBy;
    private LocalDateTime auditAt;
    private LocalDateTime submittedAt;
    private LocalDateTime broadcastAt;
    private LocalDateTime confirmedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
