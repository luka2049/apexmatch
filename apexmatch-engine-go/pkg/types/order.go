package types

import (
	"time"
	"github.com/shopspring/decimal"
)

type Order struct {
	OrderID          int64           `json:"orderId"`
	ClientOrderID    string          `json:"clientOrderId"`
	UserID           int64           `json:"userId"`
	Symbol           string          `json:"symbol"`
	Side             OrderSide       `json:"side"`
	Type             OrderType       `json:"type"`
	TimeInForce      TimeInForce     `json:"timeInForce"`
	Price            decimal.Decimal `json:"price"`
	Quantity         decimal.Decimal `json:"quantity"`
	FilledQuantity   decimal.Decimal `json:"filledQuantity"`
	TriggerPrice     decimal.Decimal `json:"triggerPrice"`
	TakeProfitPrice  decimal.Decimal `json:"takeProfitPrice"`
	StopLossPrice    decimal.Decimal `json:"stopLossPrice"`
	Status           OrderStatus     `json:"status"`
	DisplayQuantity  decimal.Decimal `json:"displayQuantity"`
	SequenceTime     int64           `json:"sequenceTime"`
	CreatedAt        time.Time       `json:"createdAt"`
}

type Trade struct {
	TradeID   int64           `json:"tradeId"`
	OrderID   int64           `json:"orderId"`
	Symbol    string          `json:"symbol"`
	Side      OrderSide       `json:"side"`
	Price     decimal.Decimal `json:"price"`
	Quantity  decimal.Decimal `json:"quantity"`
	Fee       decimal.Decimal `json:"fee"`
	Timestamp int64           `json:"timestamp"`
}
