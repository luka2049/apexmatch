package com.apexmatch.blockchain.service;

import com.apexmatch.blockchain.entity.WithdrawRecord;
import com.apexmatch.fundchain.entity.CurrencyChainConfig;
import com.apexmatch.fundchain.service.FundChainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class WithdrawService {

    private final FundChainService fundChainService;
    private final Map<Long, WithdrawRecord> withdraws = new ConcurrentHashMap<>();

    public WithdrawRecord submitWithdraw(Long userId, String currencyCode, String chainCode,
                                         String toAddress, BigDecimal amount) {
        CurrencyChainConfig config = fundChainService.getConfig(currencyCode, chainCode);
        if (config == null || !config.getWithdrawEnabled()) {
            throw new RuntimeException("提现未开放");
        }
        if (amount.compareTo(config.getMinWithdraw()) < 0) {
            throw new RuntimeException("低于最小提现额度");
        }

        WithdrawRecord record = new WithdrawRecord();
        record.setWithdrawId(System.currentTimeMillis());
        record.setUserId(userId);
        record.setCurrencyCode(currencyCode);
        record.setChainCode(chainCode);
        record.setToAddress(toAddress);
        record.setAmount(amount);
        record.setFee(config.getWithdrawFee());
        record.setActualAmount(amount.subtract(config.getWithdrawFee()));
        record.setStatus("PENDING_AUDIT");
        record.setSubmittedAt(LocalDateTime.now());
        record.setCreatedAt(LocalDateTime.now());
        withdraws.put(record.getWithdrawId(), record);

        fundChainService.recordFundFlow(userId, currencyCode, chainCode, "WITHDRAW_FREEZE",
                amount.negate(), BigDecimal.ZERO, "WITHDRAW", record.getWithdrawId().toString(), null);
        log.info("提交提现: withdrawId={}, userId={}, amount={}", record.getWithdrawId(), userId, amount);
        return record;
    }

    public void auditWithdraw(Long withdrawId, String auditBy, boolean approved) {
        WithdrawRecord record = withdraws.get(withdrawId);
        if (record == null) return;
        record.setAuditBy(auditBy);
        record.setAuditAt(LocalDateTime.now());
        if (approved) {
            record.setStatus("APPROVED");
            broadcastTransaction(record);
        } else {
            record.setStatus("REJECTED");
            fundChainService.recordFundFlow(record.getUserId(), record.getCurrencyCode(),
                    record.getChainCode(), "WITHDRAW_UNFREEZE", record.getAmount(),
                    BigDecimal.ZERO, "WITHDRAW", withdrawId.toString(), null);
        }
        record.setUpdatedAt(LocalDateTime.now());
    }

    private void broadcastTransaction(WithdrawRecord record) {
        record.setTxHash("0x" + UUID.randomUUID().toString().replace("-", ""));
        record.setStatus("BROADCASTING");
        record.setBroadcastAt(LocalDateTime.now());
        log.info("广播提现交易: withdrawId={}, txHash={}", record.getWithdrawId(), record.getTxHash());
        record.setStatus("CONFIRMED");
        record.setConfirmedAt(LocalDateTime.now());
    }

    public WithdrawRecord getWithdraw(Long withdrawId) {
        return withdraws.get(withdrawId);
    }

    public List<WithdrawRecord> getUserWithdraws(Long userId) {
        return withdraws.values().stream()
                .filter(w -> w.getUserId().equals(userId))
                .toList();
    }
}
