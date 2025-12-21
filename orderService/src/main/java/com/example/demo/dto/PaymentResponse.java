package com.example.demo.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResponse(
        String id,
        String orderId,
        String userId,
        BigDecimal amount,
        String currency,
        String status,
        String paymentMethod,
        String maskedCardNumber,
        String cardHolderName,
        LocalDateTime createdAt
) {
}