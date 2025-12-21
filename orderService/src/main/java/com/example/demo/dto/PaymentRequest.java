package com.example.demo.dto;

import java.math.BigDecimal;

public record PaymentRequest(
        String orderId,
        String userId,
        BigDecimal amount,
        String currency,
        String paymentMethod,
        String cardNumber,
        String cvv,
        String expireDate,
        String cardHolderName
) {
}