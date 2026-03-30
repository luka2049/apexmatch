#!/bin/bash
# ============================================================
# ApexMatch HTTP 接口压测脚本
# 用途：对撮合引擎进行 HTTP 层面的吞吐量和延迟压测
# 前置条件：
#   1. 启动服务（建议使用压测 profile）：
#      java -jar gateway.jar --spring.profiles.active=bench
#      或 IDEA 中 VM options 加 -Dspring.profiles.active=bench
#   2. 安装 wrk：brew install wrk（macOS）
# 用法：bash scripts/bench.sh [base_url] [duration] [threads] [connections]
# ============================================================

BASE_URL="${1:-http://localhost:8080}"
DURATION="${2:-10s}"
THREADS="${3:-4}"
CONNECTIONS="${4:-100}"

echo "=========================================="
echo " ApexMatch HTTP 接口压测"
echo " 目标: ${BASE_URL}"
echo " 持续: ${DURATION}, 线程: ${THREADS}, 并发连接: ${CONNECTIONS}"
echo "=========================================="

# 检查 wrk 是否安装
if ! command -v wrk &> /dev/null; then
    echo ""
    echo "❌ 未安装 wrk，请先执行: brew install wrk"
    echo ""
    echo "也可以用 ab（Apache Bench）替代："
    echo "  ab -n 10000 -c 100 -T 'application/json' \\"
    echo "     -p /tmp/order.json ${BASE_URL}/api/v1/order/place"
    exit 1
fi

# ---------- 准备：充值测试账户 ----------
echo ""
echo "[准备] 充值测试账户..."
for uid in $(seq 1 100); do
    curl -s -X POST "${BASE_URL}/api/v1/account/${uid}/deposit?currency=USDT&amount=10000000" > /dev/null
done
echo "  ✓ 已为用户 1~100 各充值 10,000,000 USDT"

# ---------- 准备：预挂买单（让卖单可以撮合成交） ----------
echo ""
echo "[准备] 预挂 1000 笔限价买单..."
for i in $(seq 1 1000); do
    PRICE=$(echo "49000 + $((RANDOM % 2000))" | bc)
    QTY=$(echo "scale=2; ($((RANDOM % 100 + 1))) / 100" | bc)
    UID=$((RANDOM % 50 + 1))
    curl -s -X POST "${BASE_URL}/api/v1/order/place" \
      -H "Content-Type: application/json" \
      -d "{\"userId\":${UID},\"symbol\":\"BTC-USDT\",\"side\":\"BUY\",\"type\":\"LIMIT\",\"price\":${PRICE},\"quantity\":${QTY}}" > /dev/null
done
echo "  ✓ 预挂单完成"

# ---------- 创建 wrk Lua 脚本 ----------
WRK_SCRIPT=$(mktemp /tmp/apexmatch_wrk_XXXXXX.lua)

cat > "${WRK_SCRIPT}" << 'LUAEOF'
-- wrk Lua 脚本：随机生成下单请求
local counter = 0

request = function()
    counter = counter + 1
    local uid = math.random(51, 100)
    local sides = {"BUY", "SELL"}
    local side = sides[math.random(#sides)]
    local price = 49000 + math.random(2000)
    local qty = string.format("%.2f", math.random(100) / 100)

    local body = string.format(
        '{"userId":%d,"symbol":"BTC-USDT","side":"%s","type":"LIMIT","price":%d,"quantity":%s}',
        uid, side, price, qty
    )

    return wrk.format("POST", "/api/v1/order/place", {
        ["Content-Type"] = "application/json"
    }, body)
end

done = function(summary, latency, requests)
    io.write("\n----- 延迟分布 -----\n")
    io.write(string.format("  P50:  %.2f ms\n", latency:percentile(50) / 1000))
    io.write(string.format("  P90:  %.2f ms\n", latency:percentile(90) / 1000))
    io.write(string.format("  P95:  %.2f ms\n", latency:percentile(95) / 1000))
    io.write(string.format("  P99:  %.2f ms\n", latency:percentile(99) / 1000))
    io.write(string.format("  P999: %.2f ms\n", latency:percentile(99.9) / 1000))
end
LUAEOF

# ---------- 压测 1：下单接口吞吐量 ----------
echo ""
echo "=========================================="
echo "[压测 1] 下单接口 POST /api/v1/order/place"
echo "=========================================="
wrk -t"${THREADS}" -c"${CONNECTIONS}" -d"${DURATION}" \
    -s "${WRK_SCRIPT}" \
    "${BASE_URL}/api/v1/order/place"

# ---------- 压测 2：盘口查询接口 ----------
echo ""
echo "=========================================="
echo "[压测 2] 盘口查询 GET /api/v1/market/depth/BTC-USDT"
echo "=========================================="
wrk -t"${THREADS}" -c"${CONNECTIONS}" -d"${DURATION}" \
    "${BASE_URL}/api/v1/market/depth/BTC-USDT?levels=20"

# ---------- 压测 3：账户查询接口 ----------
echo ""
echo "=========================================="
echo "[压测 3] 账户查询 GET /api/v1/account/{userId}"
echo "=========================================="
wrk -t"${THREADS}" -c"${CONNECTIONS}" -d"${DURATION}" \
    "${BASE_URL}/api/v1/account/1?currency=USDT"

# 清理
rm -f "${WRK_SCRIPT}"

echo ""
echo "=========================================="
echo " 压测完成"
echo "=========================================="
