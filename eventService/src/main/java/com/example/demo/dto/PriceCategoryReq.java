package com.example.demo.dto;

import java.math.BigDecimal;

public record PriceCategoryReq(
        BigDecimal price,
        String currency,
        int totalAllocation,
        String eventId,
        String sectionId
        ) {

}
