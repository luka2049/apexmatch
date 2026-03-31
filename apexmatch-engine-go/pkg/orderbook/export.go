package orderbook

import "github.com/shopspring/decimal"

func (plm *PriceLevelMap) GetPrices() []decimal.Decimal {
	return plm.prices
}

func (ob *OrderBook) GetBuyOrders() *PriceLevelMap {
	return ob.buyOrders
}

func (ob *OrderBook) GetSellOrders() *PriceLevelMap {
	return ob.sellOrders
}
