package com.apexmatch.blockchain.controller;

import com.apexmatch.blockchain.dto.*;
import com.apexmatch.blockchain.entity.*;
import com.apexmatch.blockchain.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/blockchain")
@RequiredArgsConstructor
public class BlockchainController {

    private final DepositService depositService;
    private final WithdrawService withdrawService;

    @PostMapping("/deposit/address")
    public DepositAddress generateDepositAddress(@RequestBody GenerateAddressRequest request) {
        return depositService.generateDepositAddress(request.getUserId(),
                request.getCurrencyCode(), request.getChainCode());
    }

    @GetMapping("/deposit/{userId}")
    public List<DepositRecord> getUserDeposits(@PathVariable Long userId) {
        return depositService.getUserDeposits(userId);
    }

    @PostMapping("/withdraw/submit")
    public WithdrawRecord submitWithdraw(@RequestBody SubmitWithdrawRequest request) {
        return withdrawService.submitWithdraw(request.getUserId(), request.getCurrencyCode(),
                request.getChainCode(), request.getToAddress(), request.getAmount());
    }

    @PostMapping("/withdraw/audit")
    public void auditWithdraw(@RequestBody AuditWithdrawRequest request) {
        withdrawService.auditWithdraw(request.getWithdrawId(), request.getAuditBy(), request.isApproved());
    }

    @GetMapping("/withdraw/{userId}")
    public List<WithdrawRecord> getUserWithdraws(@PathVariable Long userId) {
        return withdrawService.getUserWithdraws(userId);
    }
}
