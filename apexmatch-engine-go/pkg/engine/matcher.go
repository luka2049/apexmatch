package engine

import (
	"github.com/apexmatch/engine-go/pkg/orderbook"
	"github.com/apexmatch/engine-go/pkg/types"
	"github.com/shopspring/decimal"
	"time"
)

func (me *MatchingEngine) match(ob *orderbook.OrderBook, order *types.Order) types.MatchResult {
	result := types.MatchResult{
		Trades:         make([]types.Trade, 0),
		AffectedOrders: make([]types.Order, 0),
	}

	if order.TimeInForce == types.TimeInForceFOK {
		if !me.canFillCompletely(ob, order) {
			order.Status = types.OrderStatusRejected
			result.RejectReason = "FOK: Cannot fill completely"
			result.AffectedOrders = append(result.AffectedOrders, *order)
			return result
		}
	}

	if order.Type == types.OrderTypeMarket {
		me.matchMarketOrder(ob, order, &result)
	} else {
		me.matchLimitOrder(ob, order, &result)
	}

	result.AffectedOrders = append(result.AffectedOrders, *order)
	return result
}

func (me *MatchingEngine) matchLimitOrder(ob *orderbook.OrderBook, order *types.Order, result *types.MatchResult) {
	var oppositeSide *orderbook.PriceLevelMap
	if order.Side == types.OrderSideBuy {
		oppositeSide = ob.GetSellOrders()
	} else {
		oppositeSide = ob.GetBuyOrders()
	}

	for _, price := range oppositeSide.GetPrices() {
		if !me.canMatch(order, price) {
			break
		}

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
