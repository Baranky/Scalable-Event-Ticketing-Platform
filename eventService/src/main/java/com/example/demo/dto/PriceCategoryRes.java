package com.example.demo.dto;

import java.math.BigDecimal;

public record PriceCategoryRes(
        String id,
        BigDecimal price,
        String currency,
        int totalAllocation,
        String eventId,
        String sectionId
        ) {

}
