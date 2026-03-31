# ApexMatch Golang Matching Engine

高性能撮合引擎 Golang 实现，通过 CGO 导出 C 接口供 Java JNA 调用。

## 构建

### 构建动态库

**macOS:**
```bash
cd cmd/lib
go build -buildmode=c-shared -o libapexmatch_go.dylib
```

**Linux:**
```bash
cd cmd/lib
go build -buildmode=c-shared -o libapexmatch_go.so
```

**Windows:**
```bash
cd cmd/lib
go build -buildmode=c-shared -o apexmatch_go.dll
```

## 功能特性

- 价格优先、时间优先撮合算法
- 支持限价单、市价单、止损单、冰山单
- 支持 GTC、IOC、FOK 时间策略
- O(1) 惰性撤单
- 线程安全
- 高性能订单簿实现

## 目录结构

```
apexmatch-engine-go/
├── pkg/
│   ├── types/       # 数据类型定义
│   ├── orderbook/   # 订单簿实现
│   └── engine/      # 撮合引擎核心
├── cmd/
│   └── lib/         # CGO 导出层
└── test/            # 测试用例
```
