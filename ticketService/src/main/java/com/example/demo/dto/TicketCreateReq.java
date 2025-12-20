package com.example.demo.dto;

import java.math.BigDecimal;

public record TicketCreateReq(
        String eventId,
        String venueId,
        String userId,
        String sectionId,
        String seatLabel,
        String priceCategoryId,
        BigDecimal purchasePrice,
        String currency
) {
}


