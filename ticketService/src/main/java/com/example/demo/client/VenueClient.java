package com.example.demo.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "venue-service")
public interface VenueClient {

    @GetMapping("/venues/{id}")
    Object getVenueById(@PathVariable("id") String id);
}


