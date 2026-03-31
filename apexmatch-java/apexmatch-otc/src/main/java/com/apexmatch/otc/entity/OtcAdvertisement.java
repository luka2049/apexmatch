package com.apexmatch.otc.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class OtcAdvertisement {
    private Long adId;
    private Long userId;
    private String tradeType;
    private String currencyCode;
    private String fiatCurrency;
    private BigDecimal price;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    private BigDecimal availableAmount;
    private String paymentMethods;
    private String remarks;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
