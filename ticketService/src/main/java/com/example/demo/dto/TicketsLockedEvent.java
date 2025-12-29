package com.example.demo.dto;

import java.math.BigDecimal;

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

