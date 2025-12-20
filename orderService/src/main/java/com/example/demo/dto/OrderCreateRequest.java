package com.example.demo.dto;

import java.util.List;

public record OrderCreateRequest(
        String userId,
        String stockId,
        int quantity,
        List<String> seatLabels,
        String idempotencyKey
) {
}
