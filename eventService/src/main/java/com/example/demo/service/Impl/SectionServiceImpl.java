package com.example.demo.service.Impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.demo.dto.SectionReq;
import com.example.demo.dto.SectionRes;
import com.example.demo.entity.Section;
import com.example.demo.entity.Venue;
import com.example.demo.repository.SectionRepository;
import com.example.demo.repository.VenueRepository;
import com.example.demo.service.SectionService;

@Service
public class SectionServiceImpl implements SectionService {

    private final SectionRepository sectionRepository;
    private final VenueRepository venueRepository;

    public SectionServiceImpl(SectionRepository sectionRepository, VenueRepository venueRepository) {
        this.sectionRepository = sectionRepository;
        this.venueRepository = venueRepository;
    }

    @Override
    public SectionRes create(SectionReq request) {
        Section section = mapToEntity(request);
        Section saved = sectionRepository.save(section);
        return mapToResponse(saved);
    }

    @Override
    public SectionRes getById(String id) {
        Section section = sectionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Section not found: " + id));
        return mapToResponse(section);
    }

    @Override
    public Section getSectionById(String id) {
        return sectionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Section not found: " + id));
    }

    @Override
    public List<SectionRes> getAll() {
        return sectionRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public void delete(String id) {
        sectionRepository.deleteById(id);
    }

    private Section mapToEntity(SectionReq request) {
        Section section = new Section();
        section.setName(request.name());
        section.setCapacity(request.capacity());
        section.setHasSeats(request.hasSeats());
        if (request.venueId() != null) {
            Venue venue = venueRepository.findById(request.venueId())
                    .orElseThrow(() -> new RuntimeException("Venue not found: " + request.venueId()));
            section.setVenue(venue);
        }
        return section;
    }

    private SectionRes mapToResponse(Section section) {
        String venueId = section.getVenue() != null ? section.getVenue().getId() : null;
        return new SectionRes(
                section.getId(),
                section.getName(),
                section.getCapacity(),
                section.isHasSeats(),
                venueId
        );
    }
}
