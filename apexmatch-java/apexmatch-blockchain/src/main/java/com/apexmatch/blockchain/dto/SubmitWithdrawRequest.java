package com.apexmatch.blockchain.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class SubmitWithdrawRequest {
    private Long userId;
    private String currencyCode;
    private String chainCode;
    private String toAddress;
    private BigDecimal amount;
}
