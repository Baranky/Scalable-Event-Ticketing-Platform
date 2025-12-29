package com.example.demo.consumer;

import com.example.demo.dto.OrderCompletedEvent;
import com.example.demo.service.TicketStockService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderCompletedConsumer {

    private final ObjectMapper objectMapper;
    private final TicketStockService ticketStockService;

    public OrderCompletedConsumer(ObjectMapper objectMapper,
                                  TicketStockService ticketStockService) {
        this.objectMapper = objectMapper;
        this.ticketStockService = ticketStockService;
    }

    @KafkaListener(topics = "order-events", groupId = "ticket-service-group")
    public void consumeOrderCompleted(String message) {
        System.out.println("📨 Received ORDER_COMPLETED event: " + message);
        try {
            OrderCompletedEvent event = objectMapper.readValue(message, OrderCompletedEvent.class);

            if ("ORDER_COMPLETED".equals(event.eventType())) {
                System.out.println("✅ Confirming sale for orderId: " + event.orderId() 
                        + ", stockId: " + event.stockId()
                        + ", quantity: " + event.quantity());

                boolean confirmed = ticketStockService.confirmSale(
                        event.stockId(),
                        event.quantity(),
                        event.orderId()
                );

                if (confirmed) {
                    System.out.println("✅ Sale confirmed successfully for orderId: " + event.orderId());
                } else {
                    System.err.println("❌ Failed to confirm sale for orderId: " + event.orderId());
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Failed to process ORDER_COMPLETED event: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

