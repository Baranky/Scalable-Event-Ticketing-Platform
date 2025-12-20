package com.example.demo.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.demo.dto.VenueReq;
import com.example.demo.dto.VenueRes;
import com.example.demo.entity.Venue;

@Service
public interface VenueService {

    VenueRes create(VenueReq request);

    VenueRes getById(String id);

    Venue getVenusById(String id);

    List<VenueRes> getAll();

    void delete(String id);
}
