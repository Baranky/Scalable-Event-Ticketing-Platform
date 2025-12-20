package com.example.demo.service.Impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.example.demo.dto.VenueReq;
import com.example.demo.dto.VenueRes;
import com.example.demo.entity.Section;
import com.example.demo.entity.Venue;
import com.example.demo.repository.VenueRepository;
import com.example.demo.service.VenueService;

@Service
public class VenueServiceImpl implements VenueService {

    private final VenueRepository venueRepository;

    public VenueServiceImpl(VenueRepository venueRepository) {
        this.venueRepository = venueRepository;
    }

    @Override
    public VenueRes create(VenueReq request) {
        Venue venue = mapToEntity(request);
        Venue saved = venueRepository.save(venue);
        return mapToResponse(saved);
    }

    @Override
    public VenueRes getById(String id) {
        Venue venue = venueRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Venue not found: " + id));
        return mapToResponse(venue);
    }

    @Override
    public Venue getEntityById(String id) {
        return venueRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Venue not found: " + id));
    }

    @Override
    public List<VenueRes> getAll() {
        return venueRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public void delete(String id) {
        venueRepository.deleteById(id);
    }

    private Venue mapToEntity(VenueReq request) {
        Venue venue = new Venue();
        venue.setName(request.name());
        venue.setCity(request.city());
        venue.setAddress(request.address());
        venue.setCapacity(request.capacity());
        venue.setLatitude(request.latitude());
        venue.setLongitude(request.longitude());
        return venue;
    }

    private VenueRes mapToResponse(Venue venue) {
        List<String> sectionIds = venue.getSections() != null
                ? venue.getSections().stream()
                        .map(Section::getId)
                        .collect(Collectors.toList())
                : List.of();

        return new VenueRes(
                venue.getId(),
                venue.getName(),
                venue.getCity(),
                venue.getAddress(),
                venue.getCapacity(),
                venue.getLatitude(),
                venue.getLongitude(),
                sectionIds
        );
    }
}
