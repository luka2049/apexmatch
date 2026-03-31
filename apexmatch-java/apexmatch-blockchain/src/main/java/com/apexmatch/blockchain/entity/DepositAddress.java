package com.apexmatch.blockchain.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class DepositAddress {
    private Long id;
    private Long userId;
    private String currencyCode;
    private String chainCode;
    private String address;
    private String memo;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
