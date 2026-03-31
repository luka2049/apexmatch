package types

import "github.com/shopspring/decimal"

type MatchResult struct {
	Trades         []Trade `json:"trades"`
	AffectedOrders []Order `json:"affectedOrders"`
	RejectReason   string  `json:"rejectReason,omitempty"`
}

type MarketDepth struct {
	Symbol string       `json:"symbol"`
	Bids   []PriceLevel `json:"bids"`
	Asks   []PriceLevel `json:"asks"`
}

type PriceLevel struct {
	Price    decimal.Decimal `json:"price"`
	Quantity decimal.Decimal `json:"quantity"`
}
