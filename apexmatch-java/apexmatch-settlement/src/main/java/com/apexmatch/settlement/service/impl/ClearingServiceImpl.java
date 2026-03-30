package com.apexmatch.settlement.service.impl;

import com.apexmatch.account.service.AccountService;
import com.apexmatch.account.service.PositionService;
import com.apexmatch.common.entity.FundLedgerEntry;
import com.apexmatch.common.entity.Trade;
import com.apexmatch.common.enums.OrderSide;
import com.apexmatch.settlement.service.ClearingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * 实时清算实现。
 * 成交后：
 * 1. 按杠杆冻结/解冻保证金
 * 2. 扣除手续费
 * 3. 更新持仓
 *
 * @author luka
 * @since 2025-03-26
 */
@Slf4j
@RequiredArgsConstructor
public class ClearingServiceImpl implements ClearingService {

    private static final BigDecimal TAKER_FEE_RATE = new BigDecimal("0.0004");
    private static final BigDecimal MAKER_FEE_RATE = new BigDecimal("0.0002");
    private static final String DEFAULT_CURRENCY = "USDT";

    private final AccountService accountService;
    private final PositionService positionService;

    @Override
    public List<FundLedgerEntry> clearTrade(Trade trade, int leverage) {
        BigDecimal turnover = trade.getPrice().multiply(trade.getQuantity());
        BigDecimal margin = turnover.divide(BigDecimal.valueOf(leverage), 8, RoundingMode.HALF_UP);

        BigDecimal takerFee = calculateFee(turnover, false);
        BigDecimal makerFee = calculateFee(turnover, true);

        // Taker 端清算
        accountService.debit(trade.getTakerUserId(), DEFAULT_CURRENCY, takerFee,
                trade.getTakerOrderId(), trade.getTradeId());

        // 检查是否为平仓操作，如果是则解冻保证金
        boolean takerIsClosing = isClosingPosition(trade.getTakerUserId(), trade.getSymbol(), OrderSide.BUY, trade.getQuantity());
        if (takerIsClosing) {
            BigDecimal unfreezeAmount = calculateUnfreezeMargin(trade.getTakerUserId(), trade.getSymbol(),
                    trade.getQuantity(), leverage);
            accountService.unfreezeMargin(trade.getTakerUserId(), DEFAULT_CURRENCY, unfreezeAmount);
            log.debug("Taker 平仓解冻保证金 userId={} amount={}", trade.getTakerUserId(), unfreezeAmount);
        }

        positionService.updateOnTrade(trade.getTakerUserId(), trade.getSymbol(),
                OrderSide.BUY, trade.getQuantity(), trade.getPrice(), leverage);

        // Maker 端清算
        accountService.debit(trade.getMakerUserId(), DEFAULT_CURRENCY, makerFee,
                trade.getMakerOrderId(), trade.getTradeId());

        boolean makerIsClosing = isClosingPosition(trade.getMakerUserId(), trade.getSymbol(), OrderSide.SELL, trade.getQuantity());
        if (makerIsClosing) {
            BigDecimal unfreezeAmount = calculateUnfreezeMargin(trade.getMakerUserId(), trade.getSymbol(),
                    trade.getQuantity(), leverage);
            accountService.unfreezeMargin(trade.getMakerUserId(), DEFAULT_CURRENCY, unfreezeAmount);
            log.debug("Maker 平仓解冻保证金 userId={} amount={}", trade.getMakerUserId(), unfreezeAmount);
        }

        positionService.updateOnTrade(trade.getMakerUserId(), trade.getSymbol(),
                OrderSide.SELL, trade.getQuantity(), trade.getPrice(), leverage);

        log.info("清算完成 tradeId={} turnover={} takerFee={} makerFee={}",
                trade.getTradeId(), turnover, takerFee, makerFee);

        List<FundLedgerEntry> result = new ArrayList<>();
        result.addAll(accountService.getLedger(trade.getTakerUserId(), DEFAULT_CURRENCY));
        result.addAll(accountService.getLedger(trade.getMakerUserId(), DEFAULT_CURRENCY));
        return result;
    }

    /**
     * 判断是否为平仓操作（成交方向与现有持仓方向相反）
     */
    private boolean isClosingPosition(long userId, String symbol, OrderSide side, BigDecimal qty) {
        com.apexmatch.common.entity.Position pos = positionService.getOrCreatePosition(userId, symbol);
        BigDecimal currentQty = pos.getQuantity();

        // 无持仓，不是平仓
        if (currentQty.signum() == 0) {
            return false;
        }

        // 买入且当前持有空仓（负数），或卖出且当前持有多仓（正数），则为平仓
        if (side == OrderSide.BUY && currentQty.signum() < 0) {
            return true;
        }
        if (side == OrderSide.SELL && currentQty.signum() > 0) {
            return true;
        }

        return false;
    }

    /**
     * 计算需要解冻的保证金（按平仓数量比例）
     */
    private BigDecimal calculateUnfreezeMargin(long userId, String symbol, BigDecimal closeQty, int leverage) {
        com.apexmatch.common.entity.Position pos = positionService.getOrCreatePosition(userId, symbol);
        BigDecimal currentQty = pos.getQuantity().abs();

        if (currentQty.signum() == 0) {
            return BigDecimal.ZERO;
        }

        // 实际平仓数量（不能超过现有持仓）
        BigDecimal actualCloseQty = closeQty.min(currentQty);

        // 计算平仓部分的保证金：(平仓数量 / 持仓数量) * 已占用保证金
        // 简化计算：平仓价值 / 杠杆
        BigDecimal entryPrice = currentQty.signum() > 0 ? pos.getLongEntryPrice() : pos.getShortEntryPrice();
        BigDecimal closeValue = entryPrice.multiply(actualCloseQty);
        return closeValue.divide(BigDecimal.valueOf(leverage), 8, RoundingMode.HALF_UP);
    }

    @Override
    public BigDecimal calculateFee(BigDecimal turnover, boolean isMaker) {
        BigDecimal rate = isMaker ? MAKER_FEE_RATE : TAKER_FEE_RATE;
        return turnover.multiply(rate).setScale(8, RoundingMode.HALF_UP);
    }
}
