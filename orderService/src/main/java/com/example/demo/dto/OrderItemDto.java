package com.example.demo.dto;

import java.math.BigDecimal;

public record OrderItemDto(
     String stockId,
     String ticketId,
     String eventId,
     String seatLabel,
     String qrCode,
     BigDecimal price
) {
}
