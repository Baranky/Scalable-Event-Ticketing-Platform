package com.example.demo.dto;

import com.example.demo.enums.PaymentMethod;

import java.math.BigDecimal;

public record PaymentReq(
        String orderId,
        String userId,
        BigDecimal amount,
        String currency,
        PaymentMethod paymentMethod,
        String cardNumber,
        String cvv,
        String expireDate,
        String cardHolderName
) {
}
