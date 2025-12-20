package com.example.demo.service.Impl;

import com.example.demo.dto.EventCreatedEvent;
import com.example.demo.dto.EventReq;
import com.example.demo.dto.EventRes;
import com.example.demo.dto.PriceCategoryDetailRes;
import com.example.demo.entity.Event;
import com.example.demo.entity.Venue;
import com.example.demo.enums.EventStatus;
import com.example.demo.repository.EventRepository;
import com.example.demo.service.EventService;
import com.example.demo.service.VenueService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final VenueService venueService;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public EventServiceImpl(EventRepository eventRepository,
            VenueService venueService,
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper) {
        this.eventRepository = eventRepository;
        this.venueService = venueService;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public EventRes create(EventReq request) {
        Event event = mapToEvent(request);
        Event saved = eventRepository.save(event);

        return mapToResponse(saved);
    }

    @Override
    public EventRes getById(String id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found: " + id));
        return mapToResponse(event);
    }

    @Override
    public Event getEventById(String id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found: " + id));
    }

    @Override
    public List<EventRes> getAll() {
        return eventRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public void delete(String id) {
        eventRepository.deleteById(id);
    }

    @Override
    public EventRes publish(String id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found: " + id));
        event.setStatus(EventStatus.PUBLISHED);
        Event saved = eventRepository.save(event);


        publishEventCreated(saved);

        return mapToResponse(saved);
    }

    @Override
    public EventRes openSales(String id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found: " + id));

        event.setStatus(EventStatus.SALES_OPEN);
        return mapToResponse(eventRepository.save(event));
    }

    private void publishEventCreated(Event event) {
        try {
            String venueId = event.getVenue() != null ? event.getVenue().getId() : null;
            List<PriceCategoryDetailRes> priceCategoryDetails = new ArrayList<>();
            if (event.getPriceCategories() != null) {
                for (var pc : event.getPriceCategories()) {
                    String sectionId = pc.getSection() != null ? pc.getSection().getId() : null;
                    priceCategoryDetails.add(new PriceCategoryDetailRes(
                            pc.getId(),
                            sectionId,
                            pc.getPrice(),
                            pc.getCurrency(),
                            pc.getTotalAllocation()
                    ));
                }
            }
            EventCreatedEvent eventData = new EventCreatedEvent(
                    "EVENT_CREATED",
                    event.getId(),
                    event.getName(),
                    venueId,
                    priceCategoryDetails
            );
            String payload = objectMapper.writeValueAsString(eventData);

            System.out.println("Publishing EVENT_CREATED to Kafka: " + payload);
            kafkaTemplate.send("event-events", "EVENT_CREATED", payload);
            System.out.println("EVENT_CREATED published successfully for event: " + event.getId());

        } catch (JsonProcessingException e) {
            System.err.println("Failed to publish EVENT_CREATED event: " + e.getMessage());
        }
    }
    private EventRes mapToResponse(Event event) {
        String status = event.getStatus() != null ? event.getStatus().name() : null;
        String venueId = event.getVenue() != null ? event.getVenue().getId() : null;
        List<String> priceCategoryIds = event.getPriceCategories() != null
                ? event.getPriceCategories().stream()
                .map(pc -> pc.getId())
                .toList()
                : List.of();
        return new EventRes(
                event.getId(),
                event.getName(),
                event.getDescription(),
                event.getImageUrl(),
                event.getEventDate(),
                event.getDoorsOpen(),
                event.getSalesStartDate(),
                event.getSalesEndDate(),
                status,
                venueId,
                priceCategoryIds,
                event.getAttributes()
        );
    }

    private Event mapToEvent(EventReq request) {
        Event event = new Event();
        event.setName(request.name());
        event.setDescription(request.description());
        event.setImageUrl(request.imageUrl());
        event.setEventDate(request.eventDate());
        event.setDoorsOpen(request.doorsOpen());
        event.setSalesStartDate(request.salesStartDate());
        event.setSalesEndDate(request.salesEndDate());
        event.setAttributes(request.attributes());

        if (request.venueId() != null) {
            Venue venue = venueService.getVenusById(request.venueId());
            event.setVenue(venue);
        }

        event.setStatus(EventStatus.DRAFT);

        return event;
    }

}
