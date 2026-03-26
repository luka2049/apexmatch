# ApexMatch 业务文档

> 高性能合约交易撮合平台 — 完整业务说明

---

## 一、项目概述

### 1.1 项目定位

ApexMatch 是一套**高性能加密货币合约交易撮合系统**，支持 Java 与 Rust 双引擎架构，面向交易所级别的撮合场景设计。系统具备以下核心能力：

- **超高吞吐量**：单交易对撮合 TPS 超过 **170 万**（Java 引擎实测）
- **超低延迟**：撮合延迟 P99 < **1 微秒**
- **双引擎可切换**：Java 引擎（开发友好）与 Rust 引擎（极致性能）通过统一接口无缝切换
- **高可用**：Raft 共识协议，Leader 宕机秒级自动切换
- **水平扩展**：一致性哈希分片，按交易对自动路由

### 1.2 技术栈总览

| 层级 | 技术选型 |
|------|----------|
| 接入层 | Spring Boot 3.2 + WebSocket + Swagger/OpenAPI 3.0 |
| 网关中间件 | 令牌桶限流 + 熔断器 + Disruptor 背压队列 |
| 核心业务层 | Java 17 + Maven 多模块 |
| Java 撮合引擎 | ConcurrentSkipListMap + ConcurrentHashMap |
| Rust 撮合引擎 | BTreeMap + HashMap + serde_json + FFI (cdylib) |
| Java-Rust 桥接 | JNA + JSON 序列化 |
| 高可用 | Raft 状态机 + TCC 分布式事务 + 本地消息表 |
| 路由分片 | 一致性哈希环 + 服务发现抽象 |

---

## 二、核心业务模块

### 2.1 撮合引擎（apexmatch-engine-java / apexmatch-engine-rs）

#### 撮合规则

| 规则 | 说明 |
|------|------|
| 价格优先 | 买单价格越高越优先，卖单价格越低越优先 |
| 时间优先 | 同一价格下，先到的订单优先成交 |
| 主动方/被动方 | 新提交的订单为 Taker，已在簿中的为 Maker |

#### 支持的订单类型

| 类型 | 代码 | 行为 |
|------|------|------|
| **限价单** | `LIMIT` | 指定价格挂单，等待撮合或部分成交 |
| **市价单** | `MARKET` | 按对手盘最优价立即成交，无剩余挂单 |
| **止损限价** | `STOP_LIMIT` | 市场价触发 `triggerPrice` 后转为限价单 |
| **止损市价** | `STOP_MARKET` | 市场价触发后转为市价单 |
| **冰山单** | `LIMIT` + `displayQuantity` | 每次仅展示部分数量，成交后补充下一片 |

#### 有效期策略

| 策略 | 代码 | 行为 |
|------|------|------|
| **GTC** | `GTC` | 长期有效直到成交或手动撤单 |
| **IOC** | `IOC` | 立即成交尽可能多的量，剩余取消 |
| **FOK** | `FOK` | 全部成交或全部取消（All-or-Nothing） |

#### 订单簿数据结构

```
买盘 (Bids)                           卖盘 (Asks)
ConcurrentSkipListMap<Price↓, Queue>   ConcurrentSkipListMap<Price↑, Queue>
┌──────────────┐                       ┌──────────────┐
│ 50100 → [O7,O3] │ ← 最高买价      │ 50200 → [O2]    │ ← 最低卖价
│ 50000 → [O1,O5] │                  │ 50300 → [O4,O8] │
│ 49900 → [O9]    │                  │ 50500 → [O6]    │
└──────────────┘                       └──────────────┘

订单索引: ConcurrentHashMap<OrderId, Order> → O(1) 查找/撤单
```

#### 撤单策略（懒删除）

1. 标记订单状态为 `CANCELED`，从索引中移除 → **O(1)** 时间复杂度
2. 撮合过程中遇到已取消的订单自动跳过并清理
3. 避免在有序树中进行 O(log n) 的删除操作

---

### 2.2 账户模块（apexmatch-account）

#### 账户模型

| 字段 | 说明 |
|------|------|
| `balance` | 可用余额 |
| `frozenBalance` | 委托冻结金额 |
| `marginMode` | 保证金模式：`CROSS`（全仓）/ `ISOLATED`（逐仓） |
| `positionMode` | 持仓模式：`ONE_WAY`（单向）/ `HEDGE`（双向对冲） |

#### 资金流转

```
充值 → balance ↑
下单 → balance ↓, frozenBalance ↑（冻结保证金）
撤单 → balance ↑, frozenBalance ↓（解冻保证金）
成交 → 手续费扣除 + 盈亏结算
提现 → balance ↓
```

#### 资金流水类型

| 类型 | 枚举 | 说明 |
|------|------|------|
| 转入 | `TRANSFER_IN` | 充值 |
| 转出 | `TRANSFER_OUT` | 提现 |
| 冻结 | `FREEZE` | 下单冻结保证金 |
| 解冻 | `UNFREEZE` | 撤单解冻 |
| 扣减 | `DEBIT` | 手续费 / 亏损 |
| 增加 | `CREDIT` | 盈利 |
| 手续费 | `FEE` | 交易手续费 |
| 资金费率 | `FUNDING_FEE` | 合约资金费率 |

---

### 2.3 持仓模块（apexmatch-account）

#### 单向持仓模式（ONE_WAY）

- 同一交易对只有一个净持仓
- 买入 = 开多 / 平空，卖出 = 开空 / 平多
- 开仓价 = 加权平均价：`(原开仓价 × 原数量 + 新价 × 新数量) / 总数量`

#### 双向持仓模式（HEDGE）

- 同一交易对可同时持有多仓和空仓
- 多仓独立开仓价、空仓独立开仓价
- 适用于对冲策略

#### 盈亏计算

| 方向 | 未实现盈亏公式 |
|------|--------------|
| 多仓 | `(标记价 - 开仓价) × 持仓量` |
| 空仓 | `(开仓价 - 标记价) × 持仓量` |

---

### 2.4 清算与结算（apexmatch-settlement）

#### 实时清算（每笔成交）

每笔成交（Trade）触发以下清算流程：

1. **计算手续费**
   - Taker 费率：0.04%（4 bps）
   - Maker 费率：0.02%（2 bps）
   - 手续费 = 成交额 × 费率

2. **扣减手续费**：从买卖双方余额中扣除

3. **更新持仓**：调用 PositionService 更新开仓价和数量

4. **生成资金流水**：记录到 FundLedgerEntry

#### 资金费率结算

- **计算公式**：资金费 = 持仓价值 × 资金费率
- 正费率：多头付给空头
- 负费率：空头付给多头
- 结算周期：每 8 小时

#### 日终对账

- 遍历所有资金流水，计算期望余额
- 与账户实际余额比对，不一致时告警

---

### 2.5 风控模块（apexmatch-risk）

#### 事前风控（下单前检查）

| 检查项 | 规则 |
|--------|------|
| 保证金检查 | `可用余额 ≥ 订单价值 / 杠杆倍数` |
| 市价单 | 使用最小单位价格计算保证金 |

#### 强制平仓

| 条件 | 说明 |
|------|------|
| 触发条件 | `保证金率 < 维持保证金率（0.05%）` |
| 保证金率 | `(可用余额 + 未实现盈亏) / 持仓保证金` |
| 执行流程 | 计算盈亏 → 结算资金 → 清空持仓 |

---

### 2.6 行情数据（apexmatch-market-data）

#### K 线聚合

每笔成交自动更新以下周期的 K 线：

| 周期 | 间隔 |
|------|------|
| 1m | 1 分钟 |
| 5m | 5 分钟 |
| 15m | 15 分钟 |
| 1h | 1 小时 |
| 4h | 4 小时 |
| 1d | 1 天 |

#### K 线字段

| 字段 | 说明 |
|------|------|
| `open` | 开盘价（周期内首笔成交价） |
| `high` | 最高价 |
| `low` | 最低价 |
| `close` | 收盘价（周期内末笔成交价） |
| `volume` | 成交量 |
| `turnover` | 成交额 |
| `tradeCount` | 成交笔数 |

---

## 三、高可用与一致性

### 3.1 Raft 共识

每个撮合分片由 3～5 个节点组成 Raft 组：

| 角色 | 行为 |
|------|------|
| **Leader** | 接收所有写请求，创建日志，复制到 Follower |
| **Follower** | 被动接收日志并应用到本地状态机 |
| **Candidate** | Leader 失联后发起选举 |

**日志复制流程**：
1. Leader 收到订单请求 → 创建 `OperationLog`
2. 复制到所有 Follower
3. 多数节点确认 → 提交日志
4. 所有节点应用到状态机（调用 `engine.submitOrder`）
5. 返回 Leader 节点的执行结果

**故障切换**：
- Leader 宕机 → Follower 检测心跳超时 → 发起选举
- 新 Leader 当选 → 继续接收请求

### 3.2 TCC 分布式事务

用于**下单扣资金**场景，保证强一致性：

| 阶段 | 操作 | 说明 |
|------|------|------|
| **Try** | `freezeMargin` | 冻结保证金（资源预留） |
| **Confirm** | `submitOrder` | 提交到撮合引擎 |
| **Cancel** | `unfreezeMargin` | 解冻保证金（回滚） |

异常处理：
- Try 失败 → 直接 Cancel
- Confirm 失败 → Cancel 解冻
- Confirm 异常 → Cancel 解冻

### 3.3 本地消息表

用于**最终一致性**场景（如行情推送、交易通知）：

| 状态 | 说明 |
|------|------|
| `PENDING` | 待发送 |
| `SENT` | 已投递到 MQ |
| `CONFIRMED` | 消费方确认 |
| `FAILED` | 超过最大重试次数（5次） |

---

## 四、路由与分片

### 4.1 一致性哈希

- 每个物理节点映射 100～200 个虚拟节点到哈希环
- 交易对通过 MD5 哈希定位到最近的虚拟节点
- 节点上线/下线仅影响相邻节点的 key 分布

### 4.2 分片管理

| 操作 | 行为 |
|------|------|
| `route(symbol)` | 首次路由计算后缓存，后续直接返回 |
| `registerNode` | 新节点加入哈希环 |
| `unregisterNode` | 节点下线，清除关联映射，重新路由 |
| `reroute(symbol)` | 强制刷新路由缓存 |

---

## 五、接入层 API

### 5.1 REST API

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/api/v1/order/place` | 下单（限价/市价） |
| `POST` | `/api/v1/order/cancel` | 撤单 |
| `GET` | `/api/v1/account/{userId}` | 查询账户余额 |
| `POST` | `/api/v1/account/{userId}/deposit` | 充值 |
| `POST` | `/api/v1/account/{userId}/withdraw` | 提现 |
| `GET` | `/api/v1/account/{userId}/ledger` | 资金流水 |
| `GET` | `/api/v1/market/depth/{symbol}` | 盘口深度 |
| `GET` | `/api/v1/market/klines/{symbol}` | K线数据 |

#### 下单请求示例

```json
{
  "userId": 1001,
  "symbol": "BTC-USDT",
  "side": "BUY",
  "type": "LIMIT",
  "timeInForce": "GTC",
  "price": 50000.00,
  "quantity": 1.5,
  "leverage": 10
}
```

#### 统一响应格式

```json
{
  "code": 0,
  "message": "success",
  "data": { ... },
  "timestamp": 1711468800000
}
```

### 5.2 WebSocket

**连接地址**：`ws://host:8080/ws/market`

**订阅行情**：
```json
{"action": "subscribe", "topic": "depth:BTC-USDT"}
{"action": "subscribe", "topic": "kline:BTC-USDT:1m"}
```

**退订**：
```json
{"action": "unsubscribe", "topic": "depth:BTC-USDT"}
```

### 5.3 网关中间件

| 组件 | 配置 | 说明 |
|------|------|------|
| 令牌桶限流 | 桶容量 1000，填充速率 500/s | 超限返回 429 |
| 熔断器 | 失败阈值 10 次，冷却 30 秒 | 触发返回 503 |
| Disruptor | 缓冲区 64K，单线程消费 | 自然背压 |

---

## 六、错误码一览

| 错误码 | 含义 |
|--------|------|
| `40001` | 参数非法 |
| `40201` | 余额不足 |
| `40202` | 保证金不足 |
| `40301` | 风控校验未通过 |
| `40302` | 触发强制平仓 |
| `40401` | 订单不存在 |
| `40402` | 账户不存在 |
| `40403` | 持仓不存在 |
| `40901` | 重复的客户端订单号 |
| `50301` | 撮合引擎未就绪 |

---

## 七、项目模块一览

| 模块 | 说明 | 核心类 |
|------|------|--------|
| `apexmatch-common` | 公共实体 / 枚举 / 工具 | Order, Trade, Account, Position, Kline |
| `apexmatch-engine-api` | 撮合引擎标准接口 | MatchingEngine |
| `apexmatch-engine-java` | Java 撮合引擎实现 | JavaMatchingEngine, OrderBook |
| `apexmatch-engine-rs` | Rust 高性能撮合引擎 | engine.rs, orderbook.rs, ffi.rs |
| `apexmatch-engine-rust-adapter` | Rust 引擎 JNA 适配器 | RustMatchingEngine |
| `apexmatch-account` | 账户与持仓管理 | AccountService, PositionService |
| `apexmatch-settlement` | 清算与结算 | ClearingService, SettlementService |
| `apexmatch-risk` | 风控与强平 | RiskControlService, LiquidationService |
| `apexmatch-market-data` | 行情数据 | KlineService |
| `apexmatch-ha` | 高可用与一致性 | RaftGroup, TccCoordinator, LocalMessageTable |
| `apexmatch-router` | 路由与分片 | ConsistentHashRing, ShardManager |
| `apexmatch-gateway` | 接入层网关 | OrderController, OrderDisruptorService |

---

## 八、性能指标

| 指标 | 实测数据 |
|------|----------|
| 纯插入 TPS（10万笔） | **1,501,148** |
| 撮合 TPS（5万对完全成交） | **1,795,501** |
| 撮合延迟 P50 | **0.21 μs** |
| 撮合延迟 P95 | **0.54 μs** |
| 撮合延迟 P99 | **0.58 μs** |
| Disruptor 4线程并发 TPS | **187,586** |
| 单元测试覆盖 | **145 个用例全部通过** |

---

## 九、快速启动

### 编译与测试

```bash
# Java 全量编译 + 测试
cd apexmatch-java
mvn clean test

# Rust 编译
cd apexmatch-engine-rs
cargo build --release

# Rust 测试
cargo test

# Rust 基准测试
cargo bench
```

### 启动网关

```bash
cd apexmatch-java
mvn -q install -DskipTests
cd apexmatch-gateway
mvn spring-boot:run
```

启动后访问：
- REST API：`http://localhost:8080/api/v1/...`
- Swagger UI：`http://localhost:8080/swagger-ui.html`
- WebSocket：`ws://localhost:8080/ws/market`
