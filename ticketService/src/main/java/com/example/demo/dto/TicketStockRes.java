package com.example.demo.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TicketStockRes(
        String id,
        String eventId,
        String venueId,
        String sectionId,
        String priceCategoryId,
        BigDecimal price,
        String currency,
        int totalCount,
        int availableCount,
        int soldCount,
        int lockedCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

