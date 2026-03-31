package com.apexmatch.blockchain.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class DepositRecord {
    private Long depositId;
    private Long userId;
    private String currencyCode;
    private String chainCode;
    private String fromAddress;
    private String toAddress;
    private BigDecimal amount;
    private String txHash;
    private Integer confirmations;
    private Integer requiredConfirmations;
    private String status;
    private LocalDateTime detectedAt;
    private LocalDateTime confirmedAt;
    private LocalDateTime creditedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
