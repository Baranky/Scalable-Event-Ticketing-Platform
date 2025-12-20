package com.example.demo.dto;

import com.example.demo.enums.PaymentMethod;
import com.example.demo.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResponse(
        String id,
        String orderId,
        String userId,
        BigDecimal amount,
        String currency,
        PaymentStatus status,
        PaymentMethod paymentMethod,
        String maskedCardNumber,
        String cardHolderName,
        LocalDateTime createdAt
) {
}


