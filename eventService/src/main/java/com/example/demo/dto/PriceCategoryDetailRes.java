package com.example.demo.dto;

import java.math.BigDecimal;

public record PriceCategoryDetailRes(
        String priceCategoryId,
        String sectionId,
        BigDecimal price,
        String currency,
        int totalAllocation
) {
}
