package com.example.demo.consumer;

import com.example.demo.service.OrderCompletedHandlerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderCompletedConsumer {

    private final ObjectMapper objectMapper;
    private final OrderCompletedHandlerService orderCompletedHandlerService;

    public OrderCompletedConsumer(ObjectMapper objectMapper,
            OrderCompletedHandlerService orderCompletedHandlerService) {
        this.objectMapper = objectMapper;
        this.orderCompletedHandlerService = orderCompletedHandlerService;
    }

    @KafkaListener(topics = "order-events", groupId = "notification-service-group")
    public void consumeOrderCompleted(String message) {
        try {
            OrderCompletedEvent event = objectMapper.readValue(message, OrderCompletedEvent.class);

            if ("ORDER_COMPLETED".equals(event.eventType())) {
                orderCompletedHandlerService.handleOrderCompleted(
                        event.orderId(),
                        event.userId(),
                        event.paymentId()
                );
            }
        } catch (Exception e) {
            System.err.println("Failed to process ORDER_COMPLETED message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private record OrderCompletedEvent(
            String eventType,
            String orderId,
            String userId,
            String paymentId
            ) {

    }
}
