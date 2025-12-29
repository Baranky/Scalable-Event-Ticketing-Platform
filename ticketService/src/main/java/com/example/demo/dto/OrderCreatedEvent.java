package com.example.demo.dto;

import java.util.List;

public record OrderCreatedEvent(
        String eventType,
        String orderId,
        String userId,
        String stockId,
        Integer quantity,
        String eventId,
        String totalAmount,
        String currency,
        String status,
        List<String> seatLabels
) {}

