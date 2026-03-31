package com.apexmatch.common.kafka;

/**
 * Kafka 主题常量
 * 定义系统中使用的所有 Kafka 主题
 */
public class KafkaTopics {

    /**
     * 订单事件主题
     */
    public static final String ORDER_EVENTS = "apexmatch.order.events";

    /**
     * 成交事件主题
     */
    public static final String TRADE_EVENTS = "apexmatch.trade.events";

    /**
     * 结算事件主题
     */
    public static final String SETTLEMENT_EVENTS = "apexmatch.settlement.events";

    /**
     * 账户事件主题
     */
    public static final String ACCOUNT_EVENTS = "apexmatch.account.events";

    /**
     * 持仓事件主题
     */
    public static final String POSITION_EVENTS = "apexmatch.position.events";

    /**
     * 风控事件主题
     */
    public static final String RISK_EVENTS = "apexmatch.risk.events";

    /**
     * 行情数据主题
     */
    public static final String MARKET_DATA = "apexmatch.market.data";

    /**
     * 订单簿快照主题
     */
    public static final String ORDERBOOK_SNAPSHOT = "apexmatch.orderbook.snapshot";

    private KafkaTopics() {
        // 工具类，禁止实例化
    }
}
