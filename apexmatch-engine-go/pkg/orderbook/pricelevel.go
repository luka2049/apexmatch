package orderbook

import (
	"container/list"
	"github.com/apexmatch/engine-go/pkg/types"
	"github.com/shopspring/decimal"
	"sort"
)

type PriceLevelMap struct {
	isBuy  bool
	levels map[string]*PriceLevel
	prices []decimal.Decimal
}

type PriceLevel struct {
	Price  decimal.Decimal
	Orders *list.List
}

func NewPriceLevelMap(isBuy bool) *PriceLevelMap {
	return &PriceLevelMap{
		isBuy:  isBuy,
		levels: make(map[string]*PriceLevel),
		prices: make([]decimal.Decimal, 0),
	}
}

func (plm *PriceLevelMap) Add(price decimal.Decimal, order *types.Order) {
	key := price.String()
	level, exists := plm.levels[key]

	if !exists {
		level = &PriceLevel{
			Price:  price,
			Orders: list.New(),
		}
		plm.levels[key] = level
		plm.prices = append(plm.prices, price)
		plm.sortPrices()
	}

	level.Orders.PushBack(order)
}

func (plm *PriceLevelMap) sortPrices() {
	if plm.isBuy {
		sort.Slice(plm.prices, func(i, j int) bool {
			return plm.prices[i].GreaterThan(plm.prices[j])
		})
	} else {
		sort.Slice(plm.prices, func(i, j int) bool {
			return plm.prices[i].LessThan(plm.prices[j])
		})
	}
}
