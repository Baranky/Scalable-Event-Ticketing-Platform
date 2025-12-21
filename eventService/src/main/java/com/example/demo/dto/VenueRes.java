package com.example.demo.dto;

import java.util.List;

public record VenueRes(
        String id,
        String name,
        String city,
        String address,
        int capacity,
        double latitude,
        double longitude
) {

}
