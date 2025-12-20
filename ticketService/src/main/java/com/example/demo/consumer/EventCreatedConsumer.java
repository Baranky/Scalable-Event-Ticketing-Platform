package com.example.demo.consumer;

import com.example.demo.service.TicketStockService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class EventCreatedConsumer {

    private final ObjectMapper objectMapper;
    private final TicketStockService ticketStockService;

    public EventCreatedConsumer(ObjectMapper objectMapper,
            TicketStockService ticketStockService) {
        this.objectMapper = objectMapper;
        this.ticketStockService = ticketStockService;
    }

    @KafkaListener(topics = "event-events", groupId = "ticket-service-group")
    public void consumeEventCreated(String message) {
        System.out.println("Received Kafka message from event-events: " + message);
        try {
            EventCreatedEvent event = objectMapper.readValue(message, EventCreatedEvent.class);

            if ("EVENT_CREATED".equals(event.eventType())) {
                System.out.println("Processing EVENT_CREATED for eventId: " + event.eventId()
                        + ", priceCategories count: "
                        + (event.priceCategories() != null ? event.priceCategories().size() : 0));

                ticketStockService.createTicketStockForEvent(
                        event.eventId(),
                        event.venueId(),
                        event.priceCategories()
                );
            }
        } catch (Exception e) {
            System.err.println("Failed to process EVENT_CREATED message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public record PriceCategoryDetail(
            String priceCategoryId,
            String sectionId,
            BigDecimal price,
            String currency,
            int totalAllocation
            ) {

    }

    private record EventCreatedEvent(
            String eventType,
            String eventId,
            String eventName,
            String venueId,
            List<PriceCategoryDetail> priceCategories
            ) {

    }
}
