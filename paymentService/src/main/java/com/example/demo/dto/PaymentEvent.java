package com.example.demo.dto;

public record PaymentEvent(
        String eventType,
        String paymentId,
        String orderId,
        String userId,
        String amount,
        String currency,
        String status,
        String timestamp
) {}