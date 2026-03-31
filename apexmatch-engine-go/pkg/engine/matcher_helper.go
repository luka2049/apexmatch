	if order.TimeInForce == types.TimeInForceGTC && order.Type != types.OrderTypeMarket {
		if order.FilledQuantity.GreaterThan(decimal.Zero) {
			order.Status = types.OrderStatusPartiallyFilled
		}
		ob.AddOrder(order)
	} else if order.TimeInForce == types.TimeInForceIOC {
		if order.FilledQuantity.GreaterThan(decimal.Zero) {
			order.Status = types.OrderStatusPartiallyFilled
		} else {
			order.Status = types.OrderStatusCanceled
		}
	}
}

func (me *MatchingEngine) matchMarketOrder(ob *orderbook.OrderBook, order *types.Order, result *types.MatchResult) {
	var oppositeSide *orderbook.PriceLevelMap
	if order.Side == types.OrderSideBuy {
		oppositeSide = ob.GetSellOrders()
	} else {
		oppositeSide = ob.GetBuyOrders()
	}

	for _, price := range oppositeSide.GetPrices() {
		orders := oppositeSide.GetOrdersAtPrice(price)
		if orders == nil {
			continue
		}

		me.matchAtPrice(order, orders, price, result)

		if order.FilledQuantity.Equal(order.Quantity) {
			order.Status = types.OrderStatusFilled
			return
		}
	}

	if order.FilledQuantity.GreaterThan(decimal.Zero) {
		order.Status = types.OrderStatusPartiallyFilled
	} else {
		order.Status = types.OrderStatusCanceled
	}
}

func (me *MatchingEngine) canMatch(order *types.Order, price decimal.Decimal) bool {
	if order.Side == types.OrderSideBuy {
		return order.Price.GreaterThanOrEqual(price)
	}
	return order.Price.LessThanOrEqual(price)
}

func (me *MatchingEngine) canFillCompletely(ob *orderbook.OrderBook, order *types.Order) bool {
	return true
}
