package com.apexmatch.otc.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class OtcOrder {
    private Long orderId;
    private Long adId;
    private Long buyerId;
    private Long sellerId;
    private String currencyCode;
    private String fiatCurrency;
    private BigDecimal price;
    private BigDecimal amount;
    private BigDecimal totalFiat;
    private String status;
    private String paymentMethod;
    private LocalDateTime paidAt;
    private LocalDateTime releasedAt;
    private LocalDateTime cancelledAt;
    private String cancelReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
