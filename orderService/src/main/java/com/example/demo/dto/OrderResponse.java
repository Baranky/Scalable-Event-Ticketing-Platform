package com.example.demo.dto;

import com.example.demo.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
     String id,
     String userId,
     OrderStatus status,
     BigDecimal totalAmount,
     String currency,
     String stockId,
     int quantity,
     String idempotencyKey,
     String cancellationReason,
     LocalDateTime createdAt,
     LocalDateTime updatedAt,
     List<OrderItemDto> items
) {
}
