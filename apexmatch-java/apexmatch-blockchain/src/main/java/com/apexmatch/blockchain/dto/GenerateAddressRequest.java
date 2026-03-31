package com.apexmatch.blockchain.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class GenerateAddressRequest {
    private Long userId;
    private String currencyCode;
    private String chainCode;
}
