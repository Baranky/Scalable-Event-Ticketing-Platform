package com.example.demo.dto;

public record OrderCompletedEvent(
        String eventType,
        String orderId,
        String userId,
        String paymentId
) {}
