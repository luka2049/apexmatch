package orderbook

import (
	"container/list"
	"sync"
	"github.com/apexmatch/engine-go/pkg/types"
	"github.com/shopspring/decimal"
)

type OrderBook struct {
	symbol     string
	buyOrders  *PriceLevelMap
	sellOrders *PriceLevelMap
	orderIndex map[int64]*types.Order
	mu         sync.RWMutex
}

func NewOrderBook(symbol string) *OrderBook {
	return &OrderBook{
		symbol:     symbol,
		buyOrders:  NewPriceLevelMap(true),
		sellOrders: NewPriceLevelMap(false),
		orderIndex: make(map[int64]*types.Order),
	}
}

func (ob *OrderBook) AddOrder(order *types.Order) {
	ob.mu.Lock()
	defer ob.mu.Unlock()

	ob.orderIndex[order.OrderID] = order

	if order.Side == types.OrderSideBuy {
		ob.buyOrders.Add(order.Price, order)
	} else {
		ob.sellOrders.Add(order.Price, order)
	}
}

func (ob *OrderBook) RemoveOrder(orderID int64) *types.Order {
	ob.mu.Lock()
	defer ob.mu.Unlock()

	order, exists := ob.orderIndex[orderID]
	if !exists {
		return nil
	}

	order.Status = types.OrderStatusCanceled
	delete(ob.orderIndex, orderID)
	return order
}
