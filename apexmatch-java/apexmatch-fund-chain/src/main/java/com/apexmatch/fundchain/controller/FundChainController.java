package com.apexmatch.fundchain.controller;

import com.apexmatch.fundchain.entity.*;
import com.apexmatch.fundchain.service.FundChainService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/fund-chain")
@RequiredArgsConstructor
public class FundChainController {

    private final FundChainService fundChainService;

    @GetMapping("/currencies")
    public List<Currency> getAllCurrencies() {
        return fundChainService.getAllCurrencies();
    }

    @GetMapping("/chains")
    public List<Chain> getAllChains() {
        return fundChainService.getAllChains();
    }

    @GetMapping("/config/{currencyCode}")
    public List<CurrencyChainConfig> getConfigsByCurrency(@PathVariable String currencyCode) {
        return fundChainService.getConfigsByCurrency(currencyCode);
    }

    @GetMapping("/flows/{userId}/{currencyCode}")
    public List<FundFlow> getFundFlows(@PathVariable Long userId, @PathVariable String currencyCode) {
        return fundChainService.getFundFlows(userId, currencyCode);
    }
}
