package com.example.demo.service.Impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.demo.dto.PriceCategoryReq;
import com.example.demo.dto.PriceCategoryRes;
import com.example.demo.entity.Event;
import com.example.demo.entity.PriceCategory;
import com.example.demo.entity.Section;
import com.example.demo.repository.PriceCategoryRepository;
import com.example.demo.service.EventService;
import com.example.demo.service.PriceCategoryService;
import com.example.demo.service.SectionService;

@Service
public class PriceCategoryServiceImpl implements PriceCategoryService {

    private final PriceCategoryRepository priceCategoryRepository;
    private final EventService eventService;
    private final SectionService sectionService;

    public PriceCategoryServiceImpl(PriceCategoryRepository priceCategoryRepository,
            EventService eventService,
            SectionService sectionService) {
        this.priceCategoryRepository = priceCategoryRepository;
        this.eventService = eventService;
        this.sectionService = sectionService;
    }

    @Override
    public PriceCategoryRes create(PriceCategoryReq request) {
        PriceCategory entity = mapToEntity(request);
        PriceCategory saved = priceCategoryRepository.save(entity);
        return mapToResponse(saved);
    }

    @Override
    public PriceCategoryRes getById(String id) {
        PriceCategory category = priceCategoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("PriceCategory not found: " + id));
        return mapToResponse(category);
    }

    @Override
    public List<PriceCategoryRes> getAll() {
        return priceCategoryRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public void delete(String id) {
        priceCategoryRepository.deleteById(id);
    }

    private PriceCategory mapToEntity(PriceCategoryReq request) {
        PriceCategory category = new PriceCategory();
        category.setPrice(request.price());
        category.setCurrency(request.currency());
        category.setTotalAllocation(request.totalAllocation());

        // Event'i yükle ve set et
        if (request.eventId() != null) {
            Event event = eventService.getEntityById(request.eventId());
            category.setEvent(event);
        }

        // Section'ı yükle ve set et (opsiyonel)
        if (request.sectionId() != null) {
            Section section = sectionService.getEntityById(request.sectionId());
            category.setSection(section);
        }

        return category;
    }

    private PriceCategoryRes mapToResponse(PriceCategory category) {
        String eventId = category.getEvent() != null ? category.getEvent().getId() : null;
        String sectionId = category.getSection() != null ? category.getSection().getId() : null;

        return new PriceCategoryRes(
                category.getId(),
                category.getPrice(),
                category.getCurrency(),
                category.getTotalAllocation(),
                eventId,
                sectionId
        );
    }
}
