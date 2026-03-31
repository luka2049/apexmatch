package com.apexmatch.engine.golang;

import com.apexmatch.common.entity.Order;
import com.apexmatch.engine.api.EngineInitOptions;
import com.apexmatch.engine.api.MatchingEngine;
import com.apexmatch.engine.api.dto.MatchResultDTO;
import com.apexmatch.engine.api.dto.MarketDepthDTO;
import com.google.gson.Gson;
import com.sun.jna.Pointer;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class GolangMatchingEngine implements MatchingEngine {

    private final GolangNativeLibrary nativeLib;
    private final Gson gson = new Gson();

    public GolangMatchingEngine(String libraryPath) {
        log.info("Loading Golang matching engine from: {}", libraryPath);
        this.nativeLib = GolangNativeLibrary.load(libraryPath);
        log.info("Golang matching engine loaded successfully");
    }

    @Override
    public void init(String symbol, EngineInitOptions options) {
        log.info("Initializing Golang engine for symbol: {}", symbol);
        nativeLib.engine_init(symbol);
    }

    @Override
    public MatchResultDTO submitOrder(Order order) {
        String orderJson = gson.toJson(order);
        Pointer resultPtr = nativeLib.engine_submit_order_json(orderJson);
        String resultJson = resultPtr.getString(0, "UTF-8");
        nativeLib.engine_free_string(resultPtr);

        MatchResultDTO result = gson.fromJson(resultJson, MatchResultDTO.class);
        log.debug("Order {} submitted, trades: {}", order.getOrderId(),
                  result.getTrades() != null ? result.getTrades().size() : 0);
        return result;
    }
