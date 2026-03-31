package engine_test

import (
	"testing"
	"github.com/apexmatch/engine-go/pkg/engine"
	"github.com/apexmatch/engine-go/pkg/types"
	"github.com/shopspring/decimal"
)

func TestMatchingEngine_LimitOrder(t *testing.T) {
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
		Quantity: decimal.NewFromInt(1),
		FilledQuantity: decimal.Zero,
	}

	result := eng.SubmitOrder(sellOrder)
	if len(result.Trades) != 0 {
		t.Errorf("Expected no trades, got %d", len(result.Trades))
	}
	if sellOrder.Status != types.OrderStatusNew {
		t.Errorf("Expected status NEW, got %s", sellOrder.Status)
	}

	buyOrder := &types.Order{
		OrderID:  2,
		UserID:   101,
		Symbol:   "BTCUSDT",
		Side:     types.OrderSideBuy,
		Type:     types.OrderTypeLimit,
		TimeInForce: types.TimeInForceGTC,
		Price:    decimal.NewFromInt(50000),
		Quantity: decimal.NewFromInt(1),
		FilledQuantity: decimal.Zero,
	}

	result = eng.SubmitOrder(buyOrder)
	if len(result.Trades) != 1 {
		t.Errorf("Expected 1 trade, got %d", len(result.Trades))
	}
	if buyOrder.Status != types.OrderStatusFilled {
		t.Errorf("Expected status FILLED, got %s", buyOrder.Status)
	}
}
