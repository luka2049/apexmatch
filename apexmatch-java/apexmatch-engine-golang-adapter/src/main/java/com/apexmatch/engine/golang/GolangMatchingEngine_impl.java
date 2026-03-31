    @Override
    public Optional<Boolean> cancelOrder(String symbol, long orderId) {
        byte result = nativeLib.engine_cancel_order(symbol, orderId);
        boolean success = result == 1;
        log.debug("Cancel order {} for {}: {}", orderId, symbol, success);
        return Optional.of(success);
    }

    @Override
    public MarketDepthDTO getMarketDepth(String symbol, int levels) {
        Pointer depthPtr = nativeLib.engine_get_depth_json(symbol, levels);
        String depthJson = depthPtr.getString(0, "UTF-8");
        nativeLib.engine_free_string(depthPtr);

        MarketDepthDTO depth = gson.fromJson(depthJson, MarketDepthDTO.class);
        log.debug("Market depth for {}: {} bids, {} asks", symbol,
                  depth.getBids() != null ? depth.getBids().size() : 0,
                  depth.getAsks() != null ? depth.getAsks().size() : 0);
        return depth;
    }
}
