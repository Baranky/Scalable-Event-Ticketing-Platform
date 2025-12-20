package com.example.demo.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record EventRes(
        String id,
        String name,
        String description,
        String imageUrl,
        LocalDateTime eventDate,
        LocalDateTime doorsOpen,
        LocalDateTime salesStartDate,
        LocalDateTime salesEndDate,
        String status,
        String venueId,
        List<String> priceCategoryIds,
        Map<String, Object> attributes
        ) {

}
