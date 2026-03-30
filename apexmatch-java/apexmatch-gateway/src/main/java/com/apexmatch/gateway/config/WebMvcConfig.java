package com.apexmatch.gateway.config;

import com.apexmatch.gateway.filter.CircuitBreaker;
import com.apexmatch.gateway.filter.RateLimitInterceptor;
import com.apexmatch.gateway.filter.TokenBucketRateLimiter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置：注册限流 + 熔断拦截器。
 *
 * @author luka
 * @since 2025-03-26
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${apexmatch.rate-limit.max-tokens:1000}")
    private long maxTokens;

    @Value("${apexmatch.rate-limit.refill-per-second:500}")
    private double refillPerSecond;

    @Value("${apexmatch.circuit-breaker.failure-threshold:10}")
    private int failureThreshold;

    @Value("${apexmatch.circuit-breaker.cooldown-ms:30000}")
    private long cooldownMs;

    @Bean
    public TokenBucketRateLimiter tokenBucketRateLimiter() {
        return new TokenBucketRateLimiter(maxTokens, refillPerSecond);
    }

    @Bean
    public CircuitBreaker circuitBreaker() {
        return new CircuitBreaker(failureThreshold, cooldownMs);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new RateLimitInterceptor(tokenBucketRateLimiter(), circuitBreaker()))
                .addPathPatterns("/api/**");
    }
}
