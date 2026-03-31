package com.apexmatch.blockchain.dto;

import lombok.Data;

@Data
public class AuditWithdrawRequest {
    private Long withdrawId;
    private String auditBy;
    private boolean approved;
}
