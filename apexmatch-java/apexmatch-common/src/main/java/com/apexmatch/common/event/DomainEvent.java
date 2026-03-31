package com.apexmatch.common.event;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 领域事件基类
 * 所有业务事件都应继承此类
 */
@Data
public abstract class DomainEvent {

    /**
     * 事件ID（唯一标识）
     */
    private String eventId;

    /**
     * 事件类型
     */
    private String eventType;

    /**
     * 事件发生时间
     */
    private LocalDateTime occurredAt;

    /**
     * 聚合根ID（业务实体ID）
     */
    private String aggregateId;

    /**
     * 事件版本号
     */
    private Integer version;

    /**
     * 事件来源服务
     */
    private String source;

    public DomainEvent() {
        this.eventId = java.util.UUID.randomUUID().toString();
        this.occurredAt = LocalDateTime.now();
        this.version = 1;
    }
}
