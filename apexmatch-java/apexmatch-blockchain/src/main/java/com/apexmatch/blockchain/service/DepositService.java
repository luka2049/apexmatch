package com.apexmatch.blockchain.service;

import com.apexmatch.blockchain.entity.*;
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
public class DepositService {

    private final FundChainService fundChainService;
    private final Map<Long, DepositRecord> deposits = new ConcurrentHashMap<>();
    private final Map<String, DepositAddress> addresses = new ConcurrentHashMap<>();

    public DepositAddress generateDepositAddress(Long userId, String currencyCode, String chainCode) {
        String key = userId + "_" + currencyCode + "_" + chainCode;
        DepositAddress address = new DepositAddress();
        address.setId(System.currentTimeMillis());
        address.setUserId(userId);
        address.setCurrencyCode(currencyCode);
        address.setChainCode(chainCode);
        address.setAddress("0x" + UUID.randomUUID().toString().replace("-", ""));
        address.setIsActive(true);
        address.setCreatedAt(LocalDateTime.now());
        addresses.put(key, address);
        log.info("生成充值地址: userId={}, currency={}, chain={}, address={}",
                 userId, currencyCode, chainCode, address.getAddress());
        return address;
    }

    public DepositRecord detectDeposit(String txHash, String fromAddress, String toAddress,
                                       String currencyCode, String chainCode, BigDecimal amount) {
        DepositRecord record = new DepositRecord();
        record.setDepositId(System.currentTimeMillis());
        record.setFromAddress(fromAddress);
        record.setToAddress(toAddress);
        record.setCurrencyCode(currencyCode);
        record.setChainCode(chainCode);
        record.setAmount(amount);
        record.setTxHash(txHash);
        record.setConfirmations(0);
        record.setRequiredConfirmations(12);
        record.setStatus("PENDING");
        record.setDetectedAt(LocalDateTime.now());
        record.setCreatedAt(LocalDateTime.now());
        deposits.put(record.getDepositId(), record);
        log.info("检测到充值: txHash={}, amount={} {}", txHash, amount, currencyCode);
        return record;
    }

    public void confirmDeposit(Long depositId, int confirmations) {
        DepositRecord record = deposits.get(depositId);
        if (record == null) return;
        record.setConfirmations(confirmations);
        if (confirmations >= record.getRequiredConfirmations() && "PENDING".equals(record.getStatus())) {
            record.setStatus("CONFIRMED");
            record.setConfirmedAt(LocalDateTime.now());
            creditToAccount(record);
        }
        record.setUpdatedAt(LocalDateTime.now());
    }

    private void creditToAccount(DepositRecord record) {
        fundChainService.recordFundFlow(record.getUserId(), record.getCurrencyCode(),
                record.getChainCode(), "DEPOSIT", record.getAmount(), BigDecimal.ZERO,
                "DEPOSIT", record.getDepositId().toString(), record.getTxHash());
        record.setStatus("CREDITED");
        record.setCreditedAt(LocalDateTime.now());
        log.info("充值到账: depositId={}, userId={}, amount={}",
                 record.getDepositId(), record.getUserId(), record.getAmount());
    }

    public DepositRecord getDeposit(Long depositId) {
        return deposits.get(depositId);
    }

    public List<DepositRecord> getUserDeposits(Long userId) {
        return deposits.values().stream()
                .filter(d -> d.getUserId() != null && d.getUserId().equals(userId))
                .toList();
    }
}
