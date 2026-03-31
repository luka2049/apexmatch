# ApexMatch Golang 撮合引擎使用指南

## 概述

ApexMatch 现在支持三种撮合引擎实现：
- **Java 引擎**：原生 Java 实现，跨平台，易于调试
- **Rust 引擎**：高性能 Rust 实现，TPS ≥ 150,000
- **Golang 引擎**：Golang 实现，性能优异，易于维护

## 构建 Golang 引擎

### 前置要求

- Go 1.21 或更高版本
- GCC/Clang（用于 CGO）

### 构建步骤

```bash
cd apexmatch-engine-go
./build.sh
```

构建完成后，动态库位于 `cmd/lib/` 目录：
- macOS: `libapexmatch_go.dylib`
- Linux: `libapexmatch_go.so`
- Windows: `apexmatch_go.dll`

## 配置引擎切换

编辑 `apexmatch-gateway/src/main/resources/application.yml`：

```yaml
apexmatch:
  engine:
    # 选择引擎类型：java | rust | golang
    type: golang

    # Golang 引擎动态库路径
    golang-library-path: /path/to/libapexmatch_go.dylib

    # 初始化交易对
    symbols:
      - BTC-USDT
      - ETH-USDT
```

## 运行测试

### Golang 单元测试

```bash
cd apexmatch-engine-go
go test ./test/... -v
```

### Golang 性能测试

```bash
cd apexmatch-engine-go
go test ./test/... -bench=. -benchmem
```

### Java 集成测试

```bash
cd apexmatch-java/apexmatch-engine-golang-adapter
mvn test -Dgolang.engine.test=true \
  -Dgolang.engine.library=/path/to/libapexmatch_go.dylib
```

## 性能对比

| 引擎 | 单交易对 TPS | 延迟 P99 | 内存占用 |
|------|-------------|----------|---------|
| Java | 100,000+ | < 1ms | 中等 |
| Rust | 150,000+ | < 0.5ms | 低 |
| Golang | 120,000+ | < 0.8ms | 中等 |

## 功能特性

### 已实现

- ✅ 限价单（Limit Order）
- ✅ 市价单（Market Order）
- ✅ 止损单（Stop Limit/Market）
- ✅ 时间策略（GTC/IOC/FOK）
- ✅ 价格优先、时间优先撮合
- ✅ O(1) 惰性撤单
- ✅ 盘口深度查询
- ✅ 线程安全

### 待实现

- ⏳ 冰山单（Iceberg Order）
- ⏳ WAL 持久化
- ⏳ 快照恢复

## 故障排查

### 动态库加载失败

**错误信息**：
```
java.lang.UnsatisfiedLinkError: Unable to load library
```

**解决方法**：
1. 确认动态库路径正确
2. 检查文件权限：`chmod +x libapexmatch_go.dylib`
3. macOS 需要信任动态库：`xattr -d com.apple.quarantine libapexmatch_go.dylib`

### JSON 序列化错误

**错误信息**：
```
{"rejectReason":"Invalid JSON"}
```

**解决方法**：
1. 检查 Order 对象字段是否完整
2. 确认 decimal 类型正确序列化
3. 查看 Golang 日志输出

## 开发指南

### 添加新功能

1. 在 `pkg/engine/` 中实现核心逻辑
2. 在 `cmd/lib/main.go` 中导出 C 接口
3. 在 `GolangMatchingEngine.java` 中添加 Java 调用
4. 编写单元测试和集成测试

### 调试技巧

**Golang 侧**：
```go
// 添加日志
log.Printf("Order submitted: %+v", order)
```

**Java 侧**：
```yaml
logging:
  level:
    com.apexmatch.engine.golang: DEBUG
```

## 贡献

欢迎提交 Issue 和 Pull Request！

## 许可证

与 ApexMatch 主项目保持一致
