package com.example.demo.dto;


import java.util.List;


public record EventCreatedEvent(
        String eventType,
        String eventId,
        String eventName,
        String venueId,
        List<PriceCategoryDetailRes> priceCategories
) {

}