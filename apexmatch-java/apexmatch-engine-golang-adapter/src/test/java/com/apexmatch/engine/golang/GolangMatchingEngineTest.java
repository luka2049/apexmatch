package com.apexmatch.engine.golang;

import com.apexmatch.common.entity.Order;
import com.apexmatch.common.enums.OrderSide;
import com.apexmatch.common.enums.OrderType;
import com.apexmatch.common.enums.TimeInForce;
import com.apexmatch.engine.api.EngineInitOptions;
import com.apexmatch.engine.api.dto.MatchResultDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@EnabledIfSystemProperty(named = "golang.engine.test", matches = "true")
class GolangMatchingEngineTest {

    private GolangMatchingEngine engine;

    @BeforeEach
    void setUp() {
        String libraryPath = System.getProperty("golang.engine.library");
        if (libraryPath == null) {
            libraryPath = "../../apexmatch-engine-go/cmd/lib/libapexmatch_go.dylib";
        }
        engine = new GolangMatchingEngine(libraryPath);
        engine.init("BTCUSDT", new EngineInitOptions());
    }

    @Test
    void testSubmitLimitOrder() {
        Order order = new Order();
        order.setOrderId(1L);
        order.setUserId(100L);
        order.setSymbol("BTCUSDT");
        order.setSide(OrderSide.BUY);
        order.setType(OrderType.LIMIT);
        order.setTimeInForce(TimeInForce.GTC);
        order.setPrice(new BigDecimal("50000"));
        order.setQuantity(new BigDecimal("1"));

        MatchResultDTO result = engine.submitOrder(order);
        assertNotNull(result);
    }

    @Test
    void testCancelOrder() {
        Order order = new Order();
        order.setOrderId(1L);
        order.setUserId(100L);
        order.setSymbol("BTCUSDT");
        order.setSide(OrderSide.SELL);
        order.setType(OrderType.LIMIT);
        order.setTimeInForce(TimeInForce.GTC);
        order.setPrice(new BigDecimal("50000"));
        order.setQuantity(new BigDecimal("1"));

        engine.submitOrder(order);
        assertTrue(engine.cancelOrder("BTCUSDT", 1L).orElse(false));
    }
}
