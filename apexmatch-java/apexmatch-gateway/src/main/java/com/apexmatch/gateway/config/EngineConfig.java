package com.apexmatch.gateway.config;

import com.apexmatch.engine.api.MatchingEngine;
import com.apexmatch.engine.java.JavaMatchingEngine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 撮合引擎条件化配置。
 * <p>
 * 通过 {@code apexmatch.engine.type} 配置项切换 Java 引擎和 Rust 引擎：
 * <ul>
 *   <li>{@code java}（默认）：使用 Java 原生撮合引擎</li>
 *   <li>{@code rust}：通过 JNA 调用 Rust 动态库</li>
 * </ul>
 *
 * 配置示例：
 * <pre>
 * apexmatch:
 *   engine:
 *     type: java          # java | rust
 *     rust-library-path: /opt/apexmatch/libapexmatch_engine_rs.dylib
 *     symbols:
 *       - BTC-USDT
 *       - ETH-USDT
 * </pre>
 *
 * @author ApexMatch
 * @since 2025-03-26
 */
@Slf4j
@Configuration
public class EngineConfig {

    @Value("${apexmatch.engine.symbols:BTC-USDT,ETH-USDT}")
    private List<String> symbols;

    /**
     * Java 撮合引擎（默认）。
     * 当 apexmatch.engine.type 未设置或值为 java 时生效。
     */
    @Bean
    @ConditionalOnProperty(name = "apexmatch.engine.type", havingValue = "java", matchIfMissing = true)
    public MatchingEngine javaMatchingEngine() {
        JavaMatchingEngine engine = new JavaMatchingEngine();
        symbols.forEach(s -> engine.init(s, null));
        log.info("撮合引擎启动 [Java 引擎], 交易对={}", symbols);
        return engine;
    }

    /**
     * Rust 撮合引擎。
     * 当 apexmatch.engine.type=rust 时生效，且 classpath 中存在 RustMatchingEngine 类。
     */
    @Bean
    @ConditionalOnProperty(name = "apexmatch.engine.type", havingValue = "rust")
    @ConditionalOnClass(name = "com.apexmatch.engine.rust.RustMatchingEngine")
    public MatchingEngine rustMatchingEngine(
            @Value("${apexmatch.engine.rust-library-path}") String libraryPath) {
        try {
            Class<?> clazz = Class.forName("com.apexmatch.engine.rust.RustMatchingEngine");
            MatchingEngine engine = (MatchingEngine) clazz
                    .getConstructor(String.class)
                    .newInstance(libraryPath);
            symbols.forEach(s -> engine.init(s, null));
            log.info("撮合引擎启动 [Rust 引擎], 动态库={}, 交易对={}", libraryPath, symbols);
            return engine;
        } catch (Exception e) {
            throw new IllegalStateException("无法加载 Rust 撮合引擎，请确保 apexmatch-engine-rust-adapter 在 classpath 中", e);
        }
    }
}
