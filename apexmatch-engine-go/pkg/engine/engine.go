package engine

import (
	"sync"
	"time"
	"github.com/apexmatch/engine-go/pkg/orderbook"
	"github.com/apexmatch/engine-go/pkg/types"
	"github.com/shopspring/decimal"
)

type MatchingEngine struct {
	orderBooks    map[string]*orderbook.OrderBook
	stopOrderBook map[string]*StopOrderBook
	tradeIDSeq    int64
	mu            sync.RWMutex
}

func NewMatchingEngine() *MatchingEngine {
	return &MatchingEngine{
		orderBooks:    make(map[string]*orderbook.OrderBook),
		stopOrderBook: make(map[string]*StopOrderBook),
		tradeIDSeq:    1,
	}
}

func (me *MatchingEngine) Init(symbol string) {
	me.mu.Lock()
	defer me.mu.Unlock()

	if _, exists := me.orderBooks[symbol]; !exists {
		me.orderBooks[symbol] = orderbook.NewOrderBook(symbol)
		me.stopOrderBook[symbol] = NewStopOrderBook()
	}
}

func (me *MatchingEngine) SubmitOrder(order *types.Order) types.MatchResult {
	me.mu.Lock()
	defer me.mu.Unlock()

	ob, exists := me.orderBooks[order.Symbol]
	if !exists {
		return types.MatchResult{
			RejectReason: "Symbol not initialized",
		}
	}

	order.SequenceTime = time.Now().UnixNano()
	order.Status = types.OrderStatusNew

	if order.Type == types.OrderTypeStopLimit || order.Type == types.OrderTypeStopMarket {
		me.stopOrderBook[order.Symbol].Add(order)
		return types.MatchResult{
			AffectedOrders: []types.Order{*order},
		}
	}

	return me.match(ob, order)
}
