package com.apexmatch.fundchain.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Chain {
    private Long chainId;
    private String chainName;
    private String chainCode;
    private String rpcUrl;
    private Integer confirmations;
    private Boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
