package com.example.demo.dto;

public record SectionReq(
        String name,
        int capacity,
        boolean hasSeats,
        String venueId
        ) {

}
