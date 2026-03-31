func (plm *PriceLevelMap) GetBestPrice() (decimal.Decimal, bool) {
	if len(plm.prices) == 0 {
		return decimal.Zero, false
	}
	return plm.prices[0], true
}

func (plm *PriceLevelMap) GetLevels(maxLevels int) []types.PriceLevel {
	result := make([]types.PriceLevel, 0, maxLevels)

	count := 0
	for _, price := range plm.prices {
		if count >= maxLevels {
			break
		}

		level := plm.levels[price.String()]
		totalQty := decimal.Zero

		for e := level.Orders.Front(); e != nil; e = e.Next() {
			order := e.Value.(*types.Order)
			if order.Status != types.OrderStatusCanceled {
				remaining := order.Quantity.Sub(order.FilledQuantity)
				totalQty = totalQty.Add(remaining)
			}
		}

		if totalQty.GreaterThan(decimal.Zero) {
			result = append(result, types.PriceLevel{
				Price:    price,
				Quantity: totalQty,
			})
			count++
		}
	}

	return result
}

func (plm *PriceLevelMap) GetOrdersAtPrice(price decimal.Decimal) *list.List {
	level, exists := plm.levels[price.String()]
	if !exists {
		return nil
	}
	return level.Orders
}
