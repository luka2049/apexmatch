func (me *MatchingEngine) CancelOrder(symbol string, orderID int64) bool {
	me.mu.Lock()
	defer me.mu.Unlock()

	ob, exists := me.orderBooks[symbol]
	if !exists {
		return false
	}

	order := ob.RemoveOrder(orderID)
	return order != nil
}

func (me *MatchingEngine) GetMarketDepth(symbol string, levels int) types.MarketDepth {
	me.mu.RLock()
	defer me.mu.RUnlock()

	ob, exists := me.orderBooks[symbol]
	if !exists {
		return types.MarketDepth{Symbol: symbol}
	}

	return ob.GetDepth(levels)
}
