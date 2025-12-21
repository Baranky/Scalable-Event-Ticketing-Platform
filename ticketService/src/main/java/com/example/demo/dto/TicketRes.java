package com.example.demo.dto;

import com.example.demo.enums.TicketStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TicketRes(
        String id,
        String eventId,
        String venueId,
        String userId,
        String sectionId,
        String seatLabel,
        String priceCategoryId,
        TicketStatus status,
        BigDecimal purchasePrice,
        String currency,
        String qrCode,
        LocalDateTime purchasedAt,
        LocalDateTime usedAt,
        LocalDateTime refundedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
        ) {

}
