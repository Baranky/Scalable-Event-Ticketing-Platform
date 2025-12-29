package com.example.demo.consumer;

import com.example.demo.dto.TicketsSoldEvent;
import com.example.demo.service.impl.OrderServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class TicketsSoldConsumer {

    private static final Logger log = LoggerFactory.getLogger(TicketsSoldConsumer.class);
    private final ObjectMapper objectMapper;
    private final OrderServiceImpl orderService;

    public TicketsSoldConsumer(ObjectMapper objectMapper,
                                OrderServiceImpl orderService) {
        this.objectMapper = objectMapper;
        this.orderService = orderService;
    }

    @KafkaListener(topics = "ticket-events", groupId = "order-service-group")
    public void consumeTicketsSold(String message) {
        log.info("📨 Received ticket event: {}", message);
        try {
            TicketsSoldEvent event = objectMapper.readValue(message, TicketsSoldEvent.class);

            if ("TICKETS_SOLD".equals(event.eventType())) {
                log.info("✅ Tickets sold for orderId: {}, completing order...", event.orderId());
                orderService.completeOrder(event.orderId());
                log.info("✅ Order completed successfully for orderId: {}", event.orderId());
            }
        } catch (Exception e) {
            log.error("❌ Failed to process TICKETS_SOLD event: {}", e.getMessage(), e);
        }
    }
}

