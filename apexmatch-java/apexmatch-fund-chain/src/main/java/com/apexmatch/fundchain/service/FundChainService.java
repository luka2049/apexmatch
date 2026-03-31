package com.apexmatch.fundchain.service;

import com.apexmatch.fundchain.entity.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class FundChainService {

    private final Map<String, Currency> currencies = new ConcurrentHashMap<>();
    private final Map<String, Chain> chains = new ConcurrentHashMap<>();
    private final Map<String, CurrencyChainConfig> configs = new ConcurrentHashMap<>();
    private final List<FundFlow> fundFlows = new ArrayList<>();

    public void addCurrency(Currency currency) {
        currencies.put(currency.getCurrencyCode(), currency);
        log.info("添加币种: {}", currency.getCurrencyCode());
    }

    public void addChain(Chain chain) {
        chains.put(chain.getChainCode(), chain);
        log.info("添加链: {}", chain.getChainName());
    }

    public void addCurrencyChainConfig(CurrencyChainConfig config) {
        String key = config.getCurrencyCode() + "_" + config.getChainCode();
        configs.put(key, config);
        log.info("添加币种链配置: {} on {}", config.getCurrencyCode(), config.getChainCode());
    }

    public FundFlow recordFundFlow(Long userId, String currencyCode, String chainCode,
                                   String flowType, BigDecimal amount, BigDecimal balanceBefore,
                                   String refType, String refId, String txHash) {
        FundFlow flow = new FundFlow();
        flow.setFlowId(System.currentTimeMillis());
        flow.setUserId(userId);
        flow.setCurrencyCode(currencyCode);
        flow.setChainCode(chainCode);
        flow.setFlowType(flowType);
        flow.setAmount(amount);
        flow.setBalanceBefore(balanceBefore);
        flow.setBalanceAfter(balanceBefore.add(amount));
        flow.setRefType(refType);
        flow.setRefId(refId);
        flow.setTxHash(txHash);
        flow.setCreatedAt(LocalDateTime.now());
        fundFlows.add(flow);
        log.info("记录资金流水: userId={}, currency={}, chain={}, type={}, amount={}",
                 userId, currencyCode, chainCode, flowType, amount);
        return flow;
    }

    public List<FundFlow> getFundFlows(Long userId, String currencyCode) {
        return fundFlows.stream()
                .filter(f -> f.getUserId().equals(userId) && f.getCurrencyCode().equals(currencyCode))
                .toList();
    }

    public CurrencyChainConfig getConfig(String currencyCode, String chainCode) {
        return configs.get(currencyCode + "_" + chainCode);
    }

    public List<CurrencyChainConfig> getConfigsByCurrency(String currencyCode) {
        return configs.values().stream()
                .filter(c -> c.getCurrencyCode().equals(currencyCode))
                .toList();
    }

    public Currency getCurrency(String currencyCode) {
        return currencies.get(currencyCode);
    }

    public Chain getChain(String chainCode) {
        return chains.get(chainCode);
    }

    public List<Currency> getAllCurrencies() {
        return new ArrayList<>(currencies.values());
    }

    public List<Chain> getAllChains() {
        return new ArrayList<>(chains.values());
    }
}
