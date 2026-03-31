package com.apexmatch.otc.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class OtcDispute {
    private Long disputeId;
    private Long orderId;
    private Long initiatorId;
    private String reason;
    private String evidence;
    private String status;
    private String arbitratorId;
    private String resolution;
    private LocalDateTime resolvedAt;
    private LocalDateTime createdAt;
}
