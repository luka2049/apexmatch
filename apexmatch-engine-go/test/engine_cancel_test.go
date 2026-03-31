func TestMatchingEngine_PartialFill(t *testing.T) {
	eng := engine.NewMatchingEngine()
	eng.Init("BTCUSDT")

	sellOrder := &types.Order{
		OrderID:  1,
		UserID:   100,
		Symbol:   "BTCUSDT",
		Side:     types.OrderSideSell,
		Type:     types.OrderTypeLimit,
		TimeInForce: types.TimeInForceGTC,
		Price:    decimal.NewFromInt(50000),
		Quantity: decimal.NewFromInt(10),
		FilledQuantity: decimal.Zero,
	}
	eng.SubmitOrder(sellOrder)

	buyOrder := &types.Order{
		OrderID:  2,
		UserID:   101,
		Symbol:   "BTCUSDT",
		Side:     types.OrderSideBuy,
		Type:     types.OrderTypeLimit,
		TimeInForce: types.TimeInForceGTC,
		Price:    decimal.NewFromInt(50000),
		Quantity: decimal.NewFromInt(5),
		FilledQuantity: decimal.Zero,
	}

	result := eng.SubmitOrder(buyOrder)
	if len(result.Trades) != 1 {
		t.Errorf("Expected 1 trade, got %d", len(result.Trades))
	}
	if !result.Trades[0].Quantity.Equal(decimal.NewFromInt(5)) {
		t.Errorf("Expected trade quantity 5, got %s", result.Trades[0].Quantity)
	}
	if buyOrder.Status != types.OrderStatusFilled {
		t.Errorf("Expected buyer FILLED, got %s", buyOrder.Status)
	}
}

func TestMatchingEngine_CancelOrder(t *testing.T) {
	eng := engine.NewMatchingEngine()
	eng.Init("BTCUSDT")

	order := &types.Order{
		OrderID:  1,
		UserID:   100,
		Symbol:   "BTCUSDT",
		Side:     types.OrderSideSell,
		Type:     types.OrderTypeLimit,
		TimeInForce: types.TimeInForceGTC,
		Price:    decimal.NewFromInt(50000),
		Quantity: decimal.NewFromInt(1),
		FilledQuantity: decimal.Zero,
	}
	eng.SubmitOrder(order)

	success := eng.CancelOrder("BTCUSDT", 1)
	if !success {
		t.Error("Expected cancel to succeed")
	}

	success = eng.CancelOrder("BTCUSDT", 999)
	if success {
		t.Error("Expected cancel of non-existent order to fail")
	}
}
