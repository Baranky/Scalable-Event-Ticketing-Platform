package com.example.demo.dto;

import java.math.BigDecimal;

public   record TicketStockResponse(
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
        int lockedCount
) {}