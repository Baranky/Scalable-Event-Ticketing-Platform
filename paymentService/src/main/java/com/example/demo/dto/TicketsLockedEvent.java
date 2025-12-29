package com.example.demo.dto;

public record TicketsLockedEvent(
        String eventType,
        String orderId,
        String userId,
        String stockId,
        Integer quantity,
        String totalAmount,
        String currency,
        Boolean success,
        String failureReason
) {}

