package com.example.demo.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.math.BigDecimal;
import java.time.LocalDateTime;


@FeignClient(name = "paymentService")
public interface PaymentClient {

    @PostMapping("/api/payments")
    PaymentResponse createPayment(@RequestBody PaymentRequest request);


    record PaymentRequest(
            String orderId,
            String userId,
            BigDecimal amount,
            String currency,
            String paymentMethod,
            String cardNumber,
            String cvv,
            String expireDate,
            String cardHolderName
    ) {
    }

    record PaymentResponse(
            String id,
            String orderId,
            String userId,
            BigDecimal amount,
            String currency,
            String status,
            String paymentMethod,
            String maskedCardNumber,
            String cardHolderName,
            LocalDateTime createdAt
    ) {
    }
}


