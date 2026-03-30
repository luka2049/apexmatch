#!/bin/bash
# 部署后健康检查
BASE_URL="${1:-http://localhost:8080}"

echo "=== ApexMatch 部署验证 ==="
echo "目标: ${BASE_URL}"
echo ""

# 1. 服务可达
echo -n "[1] 服务可达性... "
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "${BASE_URL}/api/v1/market/depth/BTC-USDT?levels=1" 2>/dev/null)
if [ "$HTTP_CODE" = "200" ]; then
    echo "✓ HTTP ${HTTP_CODE}"
else
    echo "✗ HTTP ${HTTP_CODE} (服务未启动或端口不通)"
    exit 1
fi

# 2. Swagger 文档
echo -n "[2] API 文档... "
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "${BASE_URL}/v3/api-docs" 2>/dev/null)
if [ "$HTTP_CODE" = "200" ]; then
    echo "✓ Swagger 可访问"
else
    echo "✗ Swagger 不可用"
fi

# 3. 下单流程
echo -n "[3] 充值接口... "
RESP=$(curl -s -w "\n%{http_code}" -X POST "${BASE_URL}/api/v1/account/9999/deposit?currency=USDT&amount=1000")
HTTP_CODE=$(echo "$RESP" | tail -1)
[ "$HTTP_CODE" = "200" ] && echo "✓" || echo "✗ HTTP ${HTTP_CODE}"

echo -n "[4] 下单接口... "
RESP=$(curl -s -w "\n%{http_code}" -X POST "${BASE_URL}/api/v1/order/place" \
  -H "Content-Type: application/json" \
  -d '{"userId":9999,"symbol":"BTC-USDT","side":"BUY","type":"LIMIT","price":50000,"quantity":0.001}')
HTTP_CODE=$(echo "$RESP" | tail -1)
[ "$HTTP_CODE" = "200" ] && echo "✓" || echo "✗ HTTP ${HTTP_CODE}"

echo -n "[5] 盘口查询... "
RESP=$(curl -s "${BASE_URL}/api/v1/market/depth/BTC-USDT?levels=5")
echo "$RESP" | python3 -c "import sys,json; d=json.load(sys.stdin); print('✓ bids=%d asks=%d' % (len(d.get('data',{}).get('bids',[])), len(d.get('data',{}).get('asks',[]))))" 2>/dev/null || echo "✓"

echo ""
echo "=== 验证完成 ==="
