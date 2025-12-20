package com.example.demo.dto;

public record VenueReq(
        String name,
        String city,
        String address,
        int capacity,
        double latitude,
        double longitude
        ) {

}
