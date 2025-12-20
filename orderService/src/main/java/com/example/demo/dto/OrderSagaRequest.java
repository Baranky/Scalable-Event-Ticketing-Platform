package com.example.demo.dto;

import java.util.List;


public record OrderSagaRequest(
        String userId,
        String stockId,
        int quantity,
        List<String> seatLabels,
        String idempotencyKey,
        
        String paymentMethod,
        String cardNumber,
        String cvv,
        String expireDate,
        String cardHolderName
) {
}

