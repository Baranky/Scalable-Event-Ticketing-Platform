package com.example.demo.dto;

public record OrderCompletedEvent(
        String eventType,
        String orderId,
        String userId,
        String stockId,
        Integer quantity,
        Integer ticketCount,
        String totalAmount,
        String currency
) {}

