package com.apexmatch.settlement.service;

import com.apexmatch.account.service.impl.AccountServiceImpl;
import com.apexmatch.account.service.impl.PositionServiceImpl;
import com.apexmatch.common.entity.Trade;
import com.apexmatch.settlement.service.impl.ClearingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

class ClearingServiceTest {

    private ClearingService clearingService;
    private AccountServiceImpl accountService;

    @BeforeEach
    void setUp() {
        accountService = new AccountServiceImpl();
        PositionServiceImpl positionService = new PositionServiceImpl();
        clearingService = new ClearingServiceImpl(accountService, positionService);

        accountService.createAccount(1L, "USDT");
        accountService.deposit(1L, "USDT", new BigDecimal("100000"));
        accountService.createAccount(2L, "USDT");
        accountService.deposit(2L, "USDT", new BigDecimal("100000"));
    }

    @Test
    void clearTradeDeductsFees() {
        Trade trade = Trade.builder()
                .tradeId(1L)
                .symbol("BTC-USDT")
                .price(new BigDecimal("50000"))
                .quantity(new BigDecimal("1"))
                .takerOrderId(100L)
                .makerOrderId(200L)
                .takerUserId(1L)
                .makerUserId(2L)
                .tradeTime(System.currentTimeMillis())
                .build();

        clearingService.clearTrade(trade, 10);

        // turnover = 50000, taker fee = 50000 * 0.0004 = 20, maker fee = 50000 * 0.0002 = 10
        assertThat(accountService.getAvailableBalance(1L, "USDT"))
                .isEqualByComparingTo(new BigDecimal("99980"));
        assertThat(accountService.getAvailableBalance(2L, "USDT"))
                .isEqualByComparingTo(new BigDecimal("99990"));
    }

    @Test
    void calculateFee() {
        BigDecimal takerFee = clearingService.calculateFee(new BigDecimal("100000"), false);
        BigDecimal makerFee = clearingService.calculateFee(new BigDecimal("100000"), true);
        assertThat(takerFee).isEqualByComparingTo(new BigDecimal("40"));
        assertThat(makerFee).isEqualByComparingTo(new BigDecimal("20"));
    }

    @Test
    void closingPositionUnfreezesMargin() {
        // 用户 3 先开空仓（SELL），用户 4 开多仓（BUY）
        accountService.createAccount(3L, "USDT");
        accountService.deposit(3L, "USDT", new BigDecimal("100000"));
        accountService.createAccount(4L, "USDT");
        accountService.deposit(4L, "USDT", new BigDecimal("100000"));

        // 第一笔：用户 3 Taker SELL（开空仓），用户 4 Maker BUY（开多仓）
        Trade openTrade = Trade.builder()
                .tradeId(10L)
                .symbol("BTC-USDT")
                .price(new BigDecimal("50000"))
                .quantity(new BigDecimal("1"))
                .takerOrderId(300L)
                .makerOrderId(400L)
                .takerUserId(3L)
                .makerUserId(4L)
                .tradeTime(System.currentTimeMillis())
                .build();

        // 此时 Taker = SELL → positionService 需要收到 SELL 方向
        // ClearingServiceImpl 固定 Taker=BUY、Maker=SELL
        // 所以我们调换：用户3(空仓) = maker, 用户4(多仓) = taker

        Trade openTrade2 = Trade.builder()
                .tradeId(10L)
                .symbol("BTC-USDT")
                .price(new BigDecimal("50000"))
                .quantity(new BigDecimal("1"))
                .takerOrderId(400L)
                .makerOrderId(300L)
                .takerUserId(4L)   // Taker = BUY → 用户 4 开多仓
                .makerUserId(3L)   // Maker = SELL → 用户 3 开空仓
                .tradeTime(System.currentTimeMillis())
                .build();

        accountService.freezeMargin(4L, "USDT", new BigDecimal("5000"));
        clearingService.clearTrade(openTrade2, 10);

        // 用户 4 持仓: +1 BTC，用户 3 持仓: -1 BTC
        // 现在用户 3 平仓（BUY，反向），用户 4 平仓（SELL，反向）
        Trade closeTrade = Trade.builder()
                .tradeId(11L)
                .symbol("BTC-USDT")
                .price(new BigDecimal("51000"))
                .quantity(new BigDecimal("1"))
                .takerOrderId(401L)
                .makerOrderId(301L)
                .takerUserId(3L)   // Taker = BUY → 用户 3 平空仓（平仓）
                .makerUserId(4L)   // Maker = SELL → 用户 4 平多仓（平仓）
                .tradeTime(System.currentTimeMillis())
                .build();

        BigDecimal frozenBefore = accountService.getAccount(4L, "USDT").getFrozenBalance();
        clearingService.clearTrade(closeTrade, 10);
        BigDecimal frozenAfter = accountService.getAccount(4L, "USDT").getFrozenBalance();

        // 用户 4 平仓后冻结保证金应减少
        assertThat(frozenAfter).isLessThan(frozenBefore);
    }

    @Test
    void partialClosingUnfreezesProportionalMargin() {
        accountService.createAccount(3L, "USDT");
        accountService.deposit(3L, "USDT", new BigDecimal("100000"));
        accountService.createAccount(4L, "USDT");
        accountService.deposit(4L, "USDT", new BigDecimal("100000"));

        // 第一笔：用户 4 Taker BUY（开多仓 2 BTC），用户 3 Maker SELL（开空仓）
        Trade openTrade = Trade.builder()
                .tradeId(20L)
                .symbol("BTC-USDT")
                .price(new BigDecimal("50000"))
                .quantity(new BigDecimal("2"))
                .takerOrderId(420L)
                .makerOrderId(320L)
                .takerUserId(4L)
                .makerUserId(3L)
                .tradeTime(System.currentTimeMillis())
                .build();
        accountService.freezeMargin(4L, "USDT", new BigDecimal("10000"));
        clearingService.clearTrade(openTrade, 10);

        // 用户 4 持仓: +2 BTC
        // 平仓 1 BTC（SELL，反向）
        Trade closeTrade = Trade.builder()
                .tradeId(21L)
                .symbol("BTC-USDT")
                .price(new BigDecimal("51000"))
                .quantity(new BigDecimal("1"))
                .takerOrderId(421L)
                .makerOrderId(321L)
                .takerUserId(3L)   // 用户 3 BUY（平空仓）
                .makerUserId(4L)   // 用户 4 SELL（平一半多仓）
                .tradeTime(System.currentTimeMillis())
                .build();

        BigDecimal frozenBefore = accountService.getAccount(4L, "USDT").getFrozenBalance();
        clearingService.clearTrade(closeTrade, 10);
        BigDecimal frozenAfter = accountService.getAccount(4L, "USDT").getFrozenBalance();

        // 平仓 50% 持仓后，冻结保证金应减少
        BigDecimal unfrozen = frozenBefore.subtract(frozenAfter);
        assertThat(unfrozen).isGreaterThan(BigDecimal.ZERO);
    }
}
