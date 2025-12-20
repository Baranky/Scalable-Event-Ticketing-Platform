package com.example.demo.dto;

public record SectionRes(
        String id,
        String name,
        int capacity,
        boolean hasSeats,
        String venueId
        ) {

}
