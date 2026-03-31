package engine

import (
	"github.com/apexmatch/engine-go/pkg/types"
	"github.com/shopspring/decimal"
)

type StopOrderBook struct {
	buyStopOrders  []*types.Order
	sellStopOrders []*types.Order
}

func NewStopOrderBook() *StopOrderBook {
	return &StopOrderBook{
		buyStopOrders:  make([]*types.Order, 0),
		sellStopOrders: make([]*types.Order, 0),
	}
}

func (sob *StopOrderBook) Add(order *types.Order) {
	if order.Side == types.OrderSideBuy {
		sob.buyStopOrders = append(sob.buyStopOrders, order)
	} else {
		sob.sellStopOrders = append(sob.sellStopOrders, order)
	}
}

func (sob *StopOrderBook) CheckTriggers(lastPrice decimal.Decimal) []*types.Order {
	triggered := make([]*types.Order, 0)

	for i := len(sob.buyStopOrders) - 1; i >= 0; i-- {
		order := sob.buyStopOrders[i]
		if lastPrice.GreaterThanOrEqual(order.TriggerPrice) {
			triggered = append(triggered, order)
			sob.buyStopOrders = append(sob.buyStopOrders[:i], sob.buyStopOrders[i+1:]...)
		}
	}

	for i := len(sob.sellStopOrders) - 1; i >= 0; i-- {
		order := sob.sellStopOrders[i]
		if lastPrice.LessThanOrEqual(order.TriggerPrice) {
			triggered = append(triggered, order)
			sob.sellStopOrders = append(sob.sellStopOrders[:i], sob.sellStopOrders[i+1:]...)
		}
	}

	return triggered
}
