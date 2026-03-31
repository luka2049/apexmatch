package engine_test

import (
	"testing"
	"time"
	"github.com/apexmatch/engine-go/pkg/engine"
	"github.com/apexmatch/engine-go/pkg/types"
	"github.com/shopspring/decimal"
)

func BenchmarkMatchingEngine_SubmitOrder(b *testing.B) {
	eng := engine.NewMatchingEngine()
	eng.Init("BTCUSDT")

	b.ResetTimer()
	for i := 0; i < b.N; i++ {
		order := &types.Order{
			OrderID:  int64(i),
			UserID:   100,
			Symbol:   "BTCUSDT",
			Side:     types.OrderSideBuy,
			Type:     types.OrderTypeLimit,
			TimeInForce: types.TimeInForceGTC,
			Price:    decimal.NewFromInt(50000),
			Quantity: decimal.NewFromInt(1),
			FilledQuantity: decimal.Zero,
		}
		eng.SubmitOrder(order)
	}
}

func BenchmarkMatchingEngine_FullMatch(b *testing.B) {
	eng := engine.NewMatchingEngine()
	eng.Init("BTCUSDT")

	for i := 0; i < 10000; i++ {
		sellOrder := &types.Order{
			OrderID:  int64(i),
			UserID:   100,
			Symbol:   "BTCUSDT",
			Side:     types.OrderSideSell,
			Type:     types.OrderTypeLimit,
			TimeInForce: types.TimeInForceGTC,
			Price:    decimal.NewFromInt(50000),
			Quantity: decimal.NewFromInt(1),
			FilledQuantity: decimal.Zero,
		}
		eng.SubmitOrder(sellOrder)
	}

	b.ResetTimer()
	start := time.Now()
	for i := 0; i < b.N; i++ {
		buyOrder := &types.Order{
			OrderID:  int64(10000 + i),
			UserID:   101,
			Symbol:   "BTCUSDT",
			Side:     types.OrderSideBuy,
			Type:     types.OrderTypeLimit,
			TimeInForce: types.TimeInForceGTC,
			Price:    decimal.NewFromInt(50000),
			Quantity: decimal.NewFromInt(1),
			FilledQuantity: decimal.Zero,
		}
		eng.SubmitOrder(buyOrder)
	}
	elapsed := time.Since(start)
	tps := float64(b.N) / elapsed.Seconds()
	b.ReportMetric(tps, "tps")
}
