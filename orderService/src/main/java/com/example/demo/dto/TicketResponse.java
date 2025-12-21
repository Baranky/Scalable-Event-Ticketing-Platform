package com.example.demo.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TicketResponse(
        String id,
        String eventId,
        String venueId,
        String userId,
        String sectionId,
        String seatLabel,
        String priceCategoryId,
        String status,
        BigDecimal purchasePrice,
        String currency,
        String qrCode,
        LocalDateTime purchasedAt
) {}