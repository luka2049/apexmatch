package com.apexmatch.otc.controller;

import com.apexmatch.otc.entity.*;
import com.apexmatch.otc.service.OtcService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/otc")
@RequiredArgsConstructor
public class OtcController {

    private final OtcService otcService;

    @PostMapping("/ad/create")
    public OtcAdvertisement createAd(@RequestBody Map<String, Object> req) {
        return otcService.createAdvertisement(
                Long.valueOf(req.get("userId").toString()),
                req.get("tradeType").toString(),
                req.get("currencyCode").toString(),
                req.get("fiatCurrency").toString(),
                new java.math.BigDecimal(req.get("price").toString()),
                new java.math.BigDecimal(req.get("minAmount").toString()),
                new java.math.BigDecimal(req.get("maxAmount").toString()),
                req.get("paymentMethods").toString()
        );
    }

    @GetMapping("/ad/list/{tradeType}")
    public List<OtcAdvertisement> listAds(@PathVariable String tradeType) {
        return otcService.getActiveAdvertisements(tradeType);
    }

    @PostMapping("/order/create")
    public OtcOrder createOrder(@RequestBody Map<String, Object> req) {
        return otcService.createOrder(
                Long.valueOf(req.get("adId").toString()),
                Long.valueOf(req.get("buyerId").toString()),
                new java.math.BigDecimal(req.get("amount").toString())
        );
    }

    @PostMapping("/order/mark-paid")
    public void markPaid(@RequestBody Map<String, Object> req) {
        otcService.markPaid(
                Long.valueOf(req.get("orderId").toString()),
                Long.valueOf(req.get("buyerId").toString())
        );
    }

    @PostMapping("/order/release")
    public void releaseAsset(@RequestBody Map<String, Object> req) {
        otcService.releaseAsset(
                Long.valueOf(req.get("orderId").toString()),
                Long.valueOf(req.get("sellerId").toString())
        );
    }

    @PostMapping("/dispute/create")
    public OtcDispute createDispute(@RequestBody Map<String, Object> req) {
        return otcService.createDispute(
                Long.valueOf(req.get("orderId").toString()),
                Long.valueOf(req.get("initiatorId").toString()),
                req.get("reason").toString()
        );
    }

    @GetMapping("/order/{userId}")
    public List<OtcOrder> getUserOrders(@PathVariable Long userId) {
        return otcService.getUserOrders(userId);
    }
}
