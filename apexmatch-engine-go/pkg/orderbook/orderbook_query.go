func (ob *OrderBook) GetBestBid() (decimal.Decimal, bool) {
	ob.mu.RLock()
	defer ob.mu.RUnlock()
	return ob.buyOrders.GetBestPrice()
}

func (ob *OrderBook) GetBestAsk() (decimal.Decimal, bool) {
	ob.mu.RLock()
	defer ob.mu.RUnlock()
	return ob.sellOrders.GetBestPrice()
}

func (ob *OrderBook) GetDepth(levels int) types.MarketDepth {
	ob.mu.RLock()
	defer ob.mu.RUnlock()

	return types.MarketDepth{
		Symbol: ob.symbol,
		Bids:   ob.buyOrders.GetLevels(levels),
		Asks:   ob.sellOrders.GetLevels(levels),
	}
}

func (ob *OrderBook) GetOrder(orderID int64) (*types.Order, bool) {
	ob.mu.RLock()
	defer ob.mu.RUnlock()
	order, exists := ob.orderIndex[orderID]
	return order, exists
}
