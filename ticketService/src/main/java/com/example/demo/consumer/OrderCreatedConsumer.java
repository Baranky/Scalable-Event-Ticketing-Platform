package com.example.demo.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.example.demo.dto.OrderCreatedEvent;
import com.example.demo.dto.TicketsLockedEvent;
import com.example.demo.service.PaymentEventHandlerService;
import com.example.demo.service.TicketStockService;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class OrderCreatedConsumer {

    private final ObjectMapper objectMapper;
    private final TicketStockService ticketStockService;
    private final PaymentEventHandlerService paymentEventHandlerService;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public OrderCreatedConsumer(ObjectMapper objectMapper,
            TicketStockService ticketStockService,
            PaymentEventHandlerService paymentEventHandlerService,
            KafkaTemplate<String, String> kafkaTemplate) {
        this.objectMapper = objectMapper;
        this.ticketStockService = ticketStockService;
        this.paymentEventHandlerService = paymentEventHandlerService;
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener(topics = "order-events", groupId = "ticket-service-group")
    public void consumeOrderCreated(String message) {
        System.out.println(" Received ORDER_CREATED event: " + message);
        try {
            OrderCreatedEvent event = objectMapper.readValue(message, OrderCreatedEvent.class);

            if ("ORDER_CREATED".equals(event.eventType())) {
                System.out.println(" Locking tickets for orderId: " + event.orderId()
                        + ", stockId: " + event.stockId()
                        + ", quantity: " + event.quantity());

                boolean locked = ticketStockService.lockTickets(
                        event.stockId(),
                        event.quantity(),
                        event.orderId(),
                        event.seatLabels()
                );


                if (locked && event.seatLabels() != null && !event.seatLabels().isEmpty()) {
                    paymentEventHandlerService.storeOrderSeatLabels(event.orderId(), event.seatLabels());
                }

                TicketsLockedEvent ticketsLockedEvent = new TicketsLockedEvent(
                        "TICKETS_LOCKED",
                        event.orderId(),
                        event.userId(),
                        event.stockId(),
                        event.quantity(),
                        event.totalAmount(),
                        event.currency(),
                        locked,
                        locked ? null : "Failed to lock tickets"
                );

                String payload = objectMapper.writeValueAsString(ticketsLockedEvent);
                kafkaTemplate.send("ticket-events", event.orderId(), payload);

                if (locked) {
                    System.out.println(" Tickets locked successfully for orderId: " + event.orderId());
                    System.out.println("TICKETS_LOCKED event sent to Kafka");
                } else {
                    System.err.println("Failed to lock tickets for orderId: " + event.orderId());
                    System.err.println("TICKETS_LOCKED (failed) event sent to Kafka");
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to process ORDER_CREATED event: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
