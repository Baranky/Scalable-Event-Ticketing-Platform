package com.example.demo.dto;

public record TicketsSoldEvent(
        String eventType,
        String orderId,
        String userId,
        String stockId,
        Integer quantity
) {}



