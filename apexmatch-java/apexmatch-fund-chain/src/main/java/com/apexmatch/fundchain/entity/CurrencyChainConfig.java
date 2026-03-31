package com.apexmatch.fundchain.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 多链代币映射 - 同一币种在不同链上的合约地址
 */
@Data
public class CurrencyChainConfig {
    private Long id;
    private Long currencyId;
    private String currencyCode;
    private Long chainId;
    private String chainCode;
    private String contractAddress;
    private Integer decimals;
    private BigDecimal minDeposit;
    private BigDecimal minWithdraw;
    private BigDecimal withdrawFee;
    private Boolean depositEnabled;
    private Boolean withdrawEnabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
