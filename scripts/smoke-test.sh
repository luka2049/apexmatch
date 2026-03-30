#!/bin/bash
# ============================================================
# ApexMatch 冒烟测试脚本
# 用途：启动后快速验证所有接口是否正常
# 用法：bash scripts/smoke-test.sh [base_url]
# ============================================================

BASE_URL="${1:-http://localhost:8080}"
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m'
PASS=0
FAIL=0

check() {
    local desc="$1"
    local status="$2"
    local body="$3"
    if [[ "$status" -ge 200 && "$status" -lt 300 ]]; then
        echo -e "  ${GREEN}✓${NC} ${desc} (HTTP ${status})"
        ((PASS++))
    else
        echo -e "  ${RED}✗${NC} ${desc} (HTTP ${status})"
        echo "    响应: ${body:0:200}"
        ((FAIL++))
    fi
}

echo "=========================================="
echo " ApexMatch 冒烟测试"
echo " 目标: ${BASE_URL}"
echo "=========================================="

# ---------- 1. 健康检查 ----------
echo ""
echo "[1/5] 账户操作"

RESP=$(curl -s -w "\n%{http_code}" "${BASE_URL}/api/v1/account/1001/deposit?currency=USDT&amount=100000" -X POST)
STATUS=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
check "充值 100000 USDT" "$STATUS" "$BODY"

RESP=$(curl -s -w "\n%{http_code}" "${BASE_URL}/api/v1/account/1002/deposit?currency=USDT&amount=100000" -X POST)
STATUS=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
check "充值用户2 100000 USDT" "$STATUS" "$BODY"

RESP=$(curl -s -w "\n%{http_code}" "${BASE_URL}/api/v1/account/1001?currency=USDT")
STATUS=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
check "查询账户余额" "$STATUS" "$BODY"
echo "    $BODY"

# ---------- 2. 下买单（挂单） ----------
echo ""
echo "[2/5] 下限价买单（挂单）"

RESP=$(curl -s -w "\n%{http_code}" "${BASE_URL}/api/v1/order/place" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1001,
    "symbol": "BTC-USDT",
    "side": "BUY",
    "type": "LIMIT",
    "timeInForce": "GTC",
    "price": 50000.00,
    "quantity": 1.0
  }')
STATUS=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
check "下限价买单 BTC@50000" "$STATUS" "$BODY"
echo "    $BODY"

# ---------- 3. 下卖单（触发撮合） ----------
echo ""
echo "[3/5] 下限价卖单（触发撮合成交）"

RESP=$(curl -s -w "\n%{http_code}" "${BASE_URL}/api/v1/order/place" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1002,
    "symbol": "BTC-USDT",
    "side": "SELL",
    "type": "LIMIT",
    "timeInForce": "GTC",
    "price": 50000.00,
    "quantity": 0.5
  }')
STATUS=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
check "下限价卖单 BTC@50000 (部分成交)" "$STATUS" "$BODY"
echo "    $BODY"

# ---------- 4. 查询盘口 ----------
echo ""
echo "[4/5] 行情查询"

RESP=$(curl -s -w "\n%{http_code}" "${BASE_URL}/api/v1/market/depth/BTC-USDT?levels=5")
STATUS=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
check "查询 BTC-USDT 盘口深度" "$STATUS" "$BODY"
echo "    $BODY"

RESP=$(curl -s -w "\n%{http_code}" "${BASE_URL}/api/v1/market/klines/BTC-USDT?interval=1m&limit=10")
STATUS=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
check "查询 BTC-USDT K线" "$STATUS" "$BODY"

# ---------- 5. 市价单 ----------
echo ""
echo "[5/5] 市价单"

RESP=$(curl -s -w "\n%{http_code}" "${BASE_URL}/api/v1/order/place" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1002,
    "symbol": "BTC-USDT",
    "side": "SELL",
    "type": "MARKET",
    "quantity": 0.3
  }')
STATUS=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
check "下市价卖单 0.3 BTC" "$STATUS" "$BODY"
echo "    $BODY"

# ---------- 汇总 ----------
echo ""
echo "=========================================="
echo -e " 结果: ${GREEN}${PASS} 通过${NC}, ${RED}${FAIL} 失败${NC}"
echo "=========================================="

exit $FAIL
