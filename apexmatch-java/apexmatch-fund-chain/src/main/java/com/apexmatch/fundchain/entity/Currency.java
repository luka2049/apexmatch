package com.apexmatch.fundchain.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class Currency {
    private Long currencyId;
    private String currencyCode;
    private String currencyName;
    private Integer decimals;
    private BigDecimal minDeposit;
    private BigDecimal minWithdraw;
    private BigDecimal withdrawFee;
    private Boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
