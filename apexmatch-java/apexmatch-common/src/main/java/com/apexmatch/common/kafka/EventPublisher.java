package com.apexmatch.common.kafka;

import com.apexmatch.common.event.DomainEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * 事件发布服务
 * 负责将领域事件发布到 Kafka
 */
@Slf4j
@Service
public class EventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public EventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * 发布事件到 Kafka
     * @param topic Kafka 主题
     * @param event 领域事件
     */
    public void publish(String topic, DomainEvent event) {
        try {
            CompletableFuture<SendResult<String, Object>> future =
                    kafkaTemplate.send(topic, event.getAggregateId(), event);

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("事件发布成功: topic={}, eventId={}, eventType={}, partition={}, offset={}",
                            topic, event.getEventId(), event.getEventType(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                } else {
                    log.error("事件发布失败: topic={}, eventId={}, eventType={}",
                            topic, event.getEventId(), event.getEventType(), ex);
                }
            });
        } catch (Exception e) {
            log.error("事件发布异常: topic={}, eventId={}", topic, event.getEventId(), e);
            throw new RuntimeException("事件发布失败", e);
        }
    }

    /**
     * 同步发布事件（阻塞等待结果）
     */
    public void publishSync(String topic, DomainEvent event) {
        try {
            SendResult<String, Object> result = kafkaTemplate.send(topic, event.getAggregateId(), event).get();
            log.info("事件同步发布成功: topic={}, eventId={}, partition={}, offset={}",
                    topic, event.getEventId(),
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset());
        } catch (Exception e) {
            log.error("事件同步发布失败: topic={}, eventId={}", topic, event.getEventId(), e);
            throw new RuntimeException("事件同步发布失败", e);
        }
    }
}

