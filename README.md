# ApexMatch — 高性能交易撮合引擎

ApexMatch 是一个支持 **Java / Rust 双引擎可切换** 的高性能交易撮合系统，涵盖撮合、账户、持仓、清算、风控、行情全链路。

---

## 目录

- [环境要求](#环境要求)
- [项目结构](#项目结构)
- [快速启动](#快速启动)
  - [1. 编译 Java 模块](#1-编译-java-模块)
  - [2. 编译 Rust 引擎（可选）](#2-编译-rust-引擎可选)
  - [3. 启动服务](#3-启动服务)
  - [4. 引擎切换](#4-引擎切换)
- [测试](#测试)
  - [Java 全量测试](#java-全量测试)
  - [按模块测试](#按模块测试)
  - [集成测试](#集成测试)
  - [性能压测](#性能压测)
  - [故障演练](#故障演练)
  - [Rust 引擎测试与基准测试](#rust-引擎测试与基准测试)
- [API 文档](#api-文档)
- [核心配置项](#核心配置项)

---

## 环境要求

| 工具      | 版本要求     |
|-----------|-------------|
| JDK       | 17+         |
| Maven     | 3.8+        |
| Rust      | 1.70+（仅 Rust 引擎需要） |
| 操作系统  | macOS / Linux |

---

## 项目结构

```
apexmatch/
├── apexmatch-java/                    # Java 主工程（Maven 多模块）
│   ├── apexmatch-common/              # 公共实体、枚举、工具类
│   ├── apexmatch-engine-api/          # 撮合引擎统一接口（SPI）
│   ├── apexmatch-engine-java/         # Java 原生撮合引擎实现
│   ├── apexmatch-engine-rust-adapter/ # Rust 引擎 JNA 适配器
│   ├── apexmatch-account/             # 账户 & 持仓服务
│   ├── apexmatch-settlement/          # 清算 & 结算服务
│   ├── apexmatch-risk/                # 风控 & 强平服务
│   ├── apexmatch-market-data/         # K 线行情服务
│   ├── apexmatch-ha/                  # 高可用（Raft / TCC / 本地消息表）
│   ├── apexmatch-router/              # 一致性哈希路由 & 分片
│   └── apexmatch-gateway/             # 接入层（REST / WebSocket / Disruptor）
├── apexmatch-engine-rs/               # Rust 高性能撮合引擎（cdylib）
└── docs/                              # 架构图 & 业务文档
```

---

## 快速启动

### 1. 编译 Java 模块

```bash
cd apexmatch-java
mvn clean install -DskipTests
```

编译产物位于 `apexmatch-gateway/target/apexmatch-gateway-1.0.0-SNAPSHOT.jar`。

### 2. 编译 Rust 引擎（可选）

如果需要使用 Rust 撮合引擎，先编译动态库：

```bash
cd apexmatch-engine-rs
cargo build --release
```

产物路径：
- macOS: `target/release/libapexmatch_engine_rs.dylib`
- Linux: `target/release/libapexmatch_engine_rs.so`

### 3. 启动服务

**使用 Java 引擎（默认，无需额外配置）：**

```bash
cd apexmatch-java
java -jar apexmatch-gateway/target/apexmatch-gateway-1.0.0-SNAPSHOT.jar
```

**使用 Rust 引擎：**

```bash
java -jar apexmatch-gateway/target/apexmatch-gateway-1.0.0-SNAPSHOT.jar \
  --apexmatch.engine.type=rust \
  --apexmatch.engine.rust-library-path=/path/to/libapexmatch_engine_rs.dylib
```

启动成功后：
- REST API：`http://localhost:8080`
- Swagger UI：`http://localhost:8080/swagger-ui.html`
- WebSocket：`ws://localhost:8080/ws/market`

### 4. 引擎切换

两套引擎实现了同一个 `MatchingEngine` 接口，通过 Spring Boot 条件化配置实现零代码切换：

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `apexmatch.engine.type` | 引擎类型：`java` 或 `rust` | `java` |
| `apexmatch.engine.rust-library-path` | Rust 动态库绝对路径（仅 `type=rust` 时需要） | — |
| `apexmatch.engine.symbols` | 启动时初始化的交易对列表 | `BTC-USDT, ETH-USDT` |

切换方式（三选一）：

```bash
# 方式一：启动参数
java -jar gateway.jar --apexmatch.engine.type=rust

# 方式二：环境变量
export APEXMATCH_ENGINE_TYPE=rust
export APEXMATCH_RUST_LIB_PATH=/opt/lib/libapexmatch_engine_rs.so
java -jar gateway.jar

# 方式三：修改 application.yml
# apexmatch.engine.type: rust
```

---

## 测试

### Java 全量测试

在 `apexmatch-java` 目录下运行全部单元测试 + 集成测试：

```bash
cd apexmatch-java
mvn test
```

当前共 **100+ 个测试用例**，覆盖以下模块：

| 模块 | 测试类 | 覆盖内容 |
|------|--------|----------|
| common | `MoneyUtilsTest`, `SnowflakeIdGeneratorTest` | 金额精度计算、雪花 ID 生成 |
| engine-java | `OrderBookTest`, `JavaMatchingEngineTest`, `WalManagerTest`, `SnapshotManagerTest` | 订单簿增删改查、限价 / 市价 / FOK / IOC / 冰山单撮合、WAL 持久化、快照恢复 |
| account | `AccountServiceTest`, `PositionServiceTest` | 余额冻结解冻、保证金计算、单向 / 双向持仓 |
| settlement | `ClearingServiceTest`, `SettlementServiceTest` | 实时清算、手续费计算、日终对账 |
| risk | `RiskControlServiceTest`, `LiquidationServiceTest` | 事前风控校验、强平流程 |
| market-data | `KlineServiceTest` | K 线聚合（1m / 5m / 15m / 1h / 4h / 1d） |
| ha | `RaftGroupTest`, `TccCoordinatorTest`, `LocalMessageTableTest` | Raft 日志复制 / Leader 选举、TCC 分布式事务、本地消息表 |
| router | `ConsistentHashRingTest`, `ShardManagerTest` | 一致性哈希环、交易对分片路由 |
| gateway | `OrderControllerTest`, `MarketDataControllerTest`, `TokenBucketRateLimiterTest`, `CircuitBreakerTest`, `OrderDisruptorServiceTest` | REST 接口、限流、熔断、Disruptor 背压 |

### 按模块测试

只运行某个模块的测试：

```bash
# 撮合引擎
mvn test -pl apexmatch-engine-java

# 账户服务
mvn test -pl apexmatch-account

# 清算服务
mvn test -pl apexmatch-settlement

# 风控服务
mvn test -pl apexmatch-risk

# 高可用模块
mvn test -pl apexmatch-ha

# 网关层
mvn test -pl apexmatch-gateway
```

### 集成测试

全链路集成测试覆盖：下单 → 撮合 → 清算 → 持仓更新 → K 线生成。

```bash
mvn test -pl apexmatch-gateway -Dtest="FullPipelineIntegrationTest"
```

测试场景包括：
- 限价单完全成交
- 部分成交 + 剩余挂单
- 市价单即时成交
- 撤单
- FOK 全部成交或全部取消

### 性能压测

单机性能基准测试，输出 TPS 和延迟分布（P50 / P95 / P99）：

```bash
mvn test -pl apexmatch-gateway -Dtest="PerformanceBenchmarkTest"
```

压测项目：
- **原始插入 TPS**：纯订单簿写入性能
- **完整撮合 TPS**：下单 + 撮合 + 出结果全流程
- **延迟百分位**：P50 / P95 / P99 延迟统计
- **Disruptor 并发提交**：多线程经 Disruptor 队列提交的吞吐量

### 故障演练

模拟分布式故障场景：

```bash
mvn test -pl apexmatch-gateway -Dtest="FaultToleranceTest"
```

演练场景：
- Raft Leader 宕机后自动选举，新 Leader 继续撮合
- Follower 崩溃后重新追赶日志，状态一致
- 熔断器触发后自动恢复
- 多次 Leader 切换后各节点数据一致性校验

### Rust 引擎测试与基准测试

```bash
cd apexmatch-engine-rs

# 单元测试
cargo test

# 基准测试（Criterion）
cargo bench
```

基准测试报告生成于 `target/criterion/` 目录，包含 HTML 报告。

---

## API 文档

启动服务后访问 Swagger UI 查看完整 API 文档：

```
http://localhost:8080/swagger-ui.html
```

### 核心接口一览

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/orders` | 下单 |
| DELETE | `/api/v1/orders` | 撤单 |
| GET | `/api/v1/orders/{orderId}` | 查询订单（预留） |
| GET | `/api/v1/account/{userId}` | 查询账户信息 |
| POST | `/api/v1/account/{userId}/deposit` | 入金 |
| GET | `/api/v1/market/depth/{symbol}` | 查询盘口深度 |
| GET | `/api/v1/market/klines` | 查询 K 线数据 |

### WebSocket 订阅

连接 `ws://localhost:8080/ws/market`，发送订阅消息：

```json
{"action": "subscribe", "topic": "depth:BTC-USDT"}
```

---

## 核心配置项

`application.yml` 完整配置参考：

```yaml
server:
  port: 8080

apexmatch:
  engine:
    type: java                    # java | rust
    rust-library-path: /path/to/libapexmatch_engine_rs.dylib
    symbols:
      - BTC-USDT
      - ETH-USDT

logging:
  level:
    com.apexmatch: INFO
```

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `server.port` | int | 8080 | HTTP 服务端口 |
| `apexmatch.engine.type` | string | java | 撮合引擎类型 |
| `apexmatch.engine.rust-library-path` | string | — | Rust 动态库路径 |
| `apexmatch.engine.symbols` | list | BTC-USDT, ETH-USDT | 初始化交易对 |
| `logging.level.com.apexmatch` | string | INFO | 日志级别 |
