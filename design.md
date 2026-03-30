# ApexMatch 技术实现方案
> 本文档明确“技术上要怎么实现”，作为编码的核心指导。

## 1. 系统总体架构
采用**分层微服务架构**，从上到下分为：接入层、调度路由层、核心业务层、引擎适配层、高可用与一致性层、基础设施层。

### 核心技术栈
| 层级 | 技术选型 |
|------|----------|
| 接入层 | Java + Spring Cloud Gateway + Netty + Epoll |
| 业务层 | Java + Spring Boot + MyBatis-Plus |
| Java撮合引擎 | Java + ConcurrentSkipListMap + Disruptor |
| Rust撮合引擎 | Rust + 自定义跳表 + Crossbeam + WAL |
| 高可用 | JRaft |
| 存储 | MySQL 8.0 + Redis Cluster + ClickHouse |
| 消息队列 | Kafka |

## 2. 核心模块技术实现
### 2.1 双引擎适配设计
- **标准接口**：定义`MatchingEngine` Java接口，包含`init`、`submitOrder`、`cancelOrder`、`getMarketDepth`。
- **Java引擎**：直接实现接口，使用`ConcurrentSkipListMap`做订单簿，Disruptor做异步队列。
- **Rust引擎**：
    - Rust端编译为`cdylib`动态库，暴露`#[no_mangle]` C ABI接口。
    - Java端通过JNA封装FFI接口，实现`MatchingEngine`，使用`bincode`做Java/Rust数据序列化。
- **切换机制**：Spring Boot `@ConditionalOnProperty`注解，通过配置切换引擎实现。

### 2.2 撮合引擎核心实现
#### Java引擎
- 单线程撮合逻辑，Disruptor无锁队列做订单输入/输出缓冲。
- 对象池复用Order对象，减少YGC；ZGC垃圾回收器降低GC停顿。

#### Rust引擎
- 自定义基于内存竞技场的跳表做订单簿，`O_DIRECT`模式写WAL日志。
- CPU亲和性绑定：通过`taskset`绑定固定核心，`isolcpus`内核参数隔离核心。

### 2.3 高可用与一致性
- **JRaft集群**：每个撮合分片1主1从，Leader接收订单，同步复制到Follower，多数写入成功后执行撮合。
- **分布式事务**：TCC模式用于强一致性场景（下单扣资金），本地消息表用于最终一致性场景（行情推送）。

### 2.4 生产部署
- **混合部署**：Rust/Java撮合引擎部署在物理机（CPU隔离、关闭Swap），Java业务层部署在K8s（HPA弹性伸缩）。
- **物理机管理**：Systemd管理进程，Consul做服务发现。

## 3. 核心接口设计
### 3.1 Java-Rust FFI接口
```rust
#[no_mangle]
pub extern "C" fn engine_create(symbol: *const c_char) -> *mut OrderBook;
#[no_mangle]
pub extern "C" fn engine_submit_order(/* ... */) -> *mut u8;
#[no_mangle]
pub extern "C" fn engine_cancel_order(/* ... */) -> bool;

### 3.2 对外 REST API
POST /api/v1/order/submit：下单
POST /api/v1/order/cancel：撤单
GET /api/v1/account/balance：查询余额

