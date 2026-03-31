package engine

import (
	"container/list"
	"github.com/apexmatch/engine-go/pkg/types"
	"github.com/shopspring/decimal"
	"time"
)

func (me *MatchingEngine) matchAtPrice(takerOrder *types.Order, makerOrders *list.List, price decimal.Decimal, result *types.MatchResult) {
	for e := makerOrders.Front(); e != nil; {
		next := e.Next()
		makerOrder := e.Value.(*types.Order)

		if makerOrder.Status == types.OrderStatusCanceled {
			makerOrders.Remove(e)
			e = next
			continue
		}

		takerRemaining := takerOrder.Quantity.Sub(takerOrder.FilledQuantity)
		makerRemaining := makerOrder.Quantity.Sub(makerOrder.FilledQuantity)

		if takerRemaining.LessThanOrEqual(decimal.Zero) {
			break
		}

		matchQty := decimal.Min(takerRemaining, makerRemaining)

		takerOrder.FilledQuantity = takerOrder.FilledQuantity.Add(matchQty)
		makerOrder.FilledQuantity = makerOrder.FilledQuantity.Add(matchQty)

		trade := types.Trade{
			TradeID:   me.nextTradeID(),
			OrderID:   takerOrder.OrderID,
			Symbol:    takerOrder.Symbol,
			Side:      takerOrder.Side,
			Price:     price,
			Quantity:  matchQty,
			Fee:       decimal.Zero,
			Timestamp: time.Now().UnixMilli(),
		}
		result.Trades = append(result.Trades, trade)

		if makerOrder.FilledQuantity.Equal(makerOrder.Quantity) {
			makerOrder.Status = types.OrderStatusFilled
			makerOrders.Remove(e)
		} else {
			makerOrder.Status = types.OrderStatusPartiallyFilled
		}

		result.AffectedOrders = append(result.AffectedOrders, *makerOrder)
		e = next
	}
}

func (me *MatchingEngine) nextTradeID() int64 {
	id := me.tradeIDSeq
	me.tradeIDSeq++
	return id
}
