package com.apexmatch.router;

import com.orbitz.consul.Consul;
import com.orbitz.consul.HealthClient;
import com.orbitz.consul.model.agent.ImmutableRegistration;
import com.orbitz.consul.model.agent.Registration;
import com.orbitz.consul.model.health.ServiceHealth;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * 基于 Consul 的服务注册与发现实现。
 * <p>
 * 使用 Consul HTTP API 实现节点注册、注销、健康检查和服务发现。
 * </p>
 *
 * @author luka
 * @since 2025-03-30
 */
@Slf4j
public class ConsulServiceRegistry implements ServiceRegistry {

    private static final String SERVICE_NAME = "apexmatch-engine";
    private static final int HEALTH_CHECK_INTERVAL_SEC = 10;
    private static final int WATCH_INTERVAL_SEC = 5;

    private final Consul consul;
    private final HealthClient healthClient;
    private final List<Consumer<List<EngineNode>>> watchers = new ArrayList<>();
    private final ScheduledExecutorService watchScheduler = Executors.newSingleThreadScheduledExecutor();

    public ConsulServiceRegistry(String consulHost, int consulPort) {
        this.consul = Consul.builder()
                .withUrl(String.format("http://%s:%d", consulHost, consulPort))
                .build();
        this.healthClient = consul.healthClient();
        startWatching();
        log.info("Consul 服务注册中心初始化完成 {}:{}", consulHost, consulPort);
    }

    @Override
    public void register(EngineNode node) {
        String serviceId = SERVICE_NAME + "-" + node.getNodeId();
        String healthCheckUrl = String.format("http://%s:%d/health", node.getHost(), node.getPort());

        Map<String, String> meta = new HashMap<>();
        meta.put("nodeId", node.getNodeId());
        meta.put("symbolCount", String.valueOf(node.getSymbolCount()));

        Registration registration = ImmutableRegistration.builder()
                .id(serviceId)
                .name(SERVICE_NAME)
                .address(node.getHost())
                .port(node.getPort())
                .check(Registration.RegCheck.http(healthCheckUrl, HEALTH_CHECK_INTERVAL_SEC))
                .meta(meta)
                .build();

        consul.agentClient().register(registration);
        node.setAlive(true);
        log.info("节点注册到 Consul serviceId={} address={}", serviceId, node.address());
    }

    @Override
    public void deregister(EngineNode node) {
        String serviceId = SERVICE_NAME + "-" + node.getNodeId();
        consul.agentClient().deregister(serviceId);
        log.info("节点从 Consul 注销 serviceId={}", serviceId);
    }

    @Override
    public List<EngineNode> getAliveNodes() {
        List<ServiceHealth> healthyServices = healthClient.getHealthyServiceInstances(SERVICE_NAME).getResponse();
        List<EngineNode> nodes = new ArrayList<>();

        for (ServiceHealth service : healthyServices) {
            String nodeId = service.getService().getMeta().get("nodeId");
            String host = service.getService().getAddress();
            int port = service.getService().getPort();
            int symbolCount = Integer.parseInt(service.getService().getMeta().getOrDefault("symbolCount", "0"));

            EngineNode node = EngineNode.builder()
                    .nodeId(nodeId)
                    .host(host)
                    .port(port)
                    .alive(true)
                    .symbolCount(symbolCount)
                    .build();
            nodes.add(node);
        }

        return nodes;
    }

    @Override
    public void watch(Consumer<List<EngineNode>> callback) {
        watchers.add(callback);
    }

    private void startWatching() {
        watchScheduler.scheduleAtFixedRate(() -> {
            try {
                List<EngineNode> aliveNodes = getAliveNodes();
                for (Consumer<List<EngineNode>> watcher : watchers) {
                    watcher.accept(aliveNodes);
                }
            } catch (Exception e) {
                log.error("Consul 服务发现监听失败", e);
            }
        }, WATCH_INTERVAL_SEC, WATCH_INTERVAL_SEC, TimeUnit.SECONDS);
    }

    public void shutdown() {
        watchScheduler.shutdown();
        consul.destroy();
    }
}

