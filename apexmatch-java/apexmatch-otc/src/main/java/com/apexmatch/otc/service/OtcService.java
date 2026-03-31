package com.apexmatch.otc.service;

import com.apexmatch.otc.entity.*;
import com.apexmatch.fundchain.service.FundChainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtcService {

    private final FundChainService fundChainService;
    private final Map<Long, OtcAdvertisement> advertisements = new ConcurrentHashMap<>();
    private final Map<Long, OtcOrder> orders = new ConcurrentHashMap<>();
    private final Map<Long, OtcDispute> disputes = new ConcurrentHashMap<>();

    public OtcAdvertisement createAdvertisement(Long userId, String tradeType, String currencyCode,
                                                String fiatCurrency, BigDecimal price, BigDecimal minAmount,
                                                BigDecimal maxAmount, String paymentMethods) {
        OtcAdvertisement ad = new OtcAdvertisement();
        ad.setAdId(System.currentTimeMillis());
        ad.setUserId(userId);
        ad.setTradeType(tradeType);
        ad.setCurrencyCode(currencyCode);
        ad.setFiatCurrency(fiatCurrency);
        ad.setPrice(price);
        ad.setMinAmount(minAmount);
        ad.setMaxAmount(maxAmount);
        ad.setAvailableAmount(maxAmount);
        ad.setPaymentMethods(paymentMethods);
        ad.setIsActive(true);
        ad.setCreatedAt(LocalDateTime.now());
        advertisements.put(ad.getAdId(), ad);
        log.info("创建 OTC 广告: adId={}, type={}, currency={}", ad.getAdId(), tradeType, currencyCode);
        return ad;
    }

    public OtcOrder createOrder(Long adId, Long buyerId, BigDecimal amount) {
        OtcAdvertisement ad = advertisements.get(adId);
        if (ad == null || !ad.getIsActive()) {
            throw new RuntimeException("广告不存在或已下架");
        }
        if (amount.compareTo(ad.getAvailableAmount()) > 0) {
            throw new RuntimeException("超过可用数量");
        }

        OtcOrder order = new OtcOrder();
        order.setOrderId(System.currentTimeMillis());
        order.setAdId(adId);
        order.setBuyerId(buyerId);
        order.setSellerId(ad.getUserId());
        order.setCurrencyCode(ad.getCurrencyCode());
        order.setFiatCurrency(ad.getFiatCurrency());
        order.setPrice(ad.getPrice());
        order.setAmount(amount);
        order.setTotalFiat(amount.multiply(ad.getPrice()));
        order.setStatus("PENDING_PAYMENT");
        order.setPaymentMethod(ad.getPaymentMethods());
        order.setCreatedAt(LocalDateTime.now());
        orders.put(order.getOrderId(), order);

        ad.setAvailableAmount(ad.getAvailableAmount().subtract(amount));
        fundChainService.recordFundFlow(ad.getUserId(), ad.getCurrencyCode(), null,
                "OTC_FREEZE", amount.negate(), BigDecimal.ZERO, "OTC", order.getOrderId().toString(), null);
        log.info("创建 OTC 订单: orderId={}, buyer={}, amount={}", order.getOrderId(), buyerId, amount);
        return order;
    }

    public void markPaid(Long orderId, Long buyerId) {
        OtcOrder order = orders.get(orderId);
        if (order == null || !order.getBuyerId().equals(buyerId)) {
            throw new RuntimeException("订单不存在或无权操作");
        }
        order.setStatus("PAID");
        order.setPaidAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        log.info("买家标记已付款: orderId={}", orderId);
    }

    public void releaseAsset(Long orderId, Long sellerId) {
        OtcOrder order = orders.get(orderId);
        if (order == null || !order.getSellerId().equals(sellerId)) {
            throw new RuntimeException("订单不存在或无权操作");
        }
        order.setStatus("COMPLETED");
        order.setReleasedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        fundChainService.recordFundFlow(order.getBuyerId(), order.getCurrencyCode(), null,
                "OTC_BUY", order.getAmount(), BigDecimal.ZERO, "OTC", orderId.toString(), null);
        log.info("卖家放币: orderId={}", orderId);
    }

    public OtcDispute createDispute(Long orderId, Long initiatorId, String reason) {
        OtcDispute dispute = new OtcDispute();
        dispute.setDisputeId(System.currentTimeMillis());
        dispute.setOrderId(orderId);
        dispute.setInitiatorId(initiatorId);
        dispute.setReason(reason);
        dispute.setStatus("OPEN");
        dispute.setCreatedAt(LocalDateTime.now());
        disputes.put(dispute.getDisputeId(), dispute);
        log.warn("创建争议: disputeId={}, orderId={}", dispute.getDisputeId(), orderId);
        return dispute;
    }

    public List<OtcAdvertisement> getActiveAdvertisements(String tradeType) {
        return advertisements.values().stream()
                .filter(ad -> ad.getIsActive() && ad.getTradeType().equals(tradeType))
                .toList();
    }

    public List<OtcOrder> getUserOrders(Long userId) {
        return orders.values().stream()
                .filter(o -> o.getBuyerId().equals(userId) || o.getSellerId().equals(userId))
                .toList();
    }
}
