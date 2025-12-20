package com.example.demo.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record EventReq(
        String name,
        String description,
        String imageUrl,
        LocalDateTime eventDate,
        LocalDateTime doorsOpen,
        LocalDateTime salesStartDate,
        LocalDateTime salesEndDate,
        String venueId,
        Map<String, Object> attributes
        ) {

}
