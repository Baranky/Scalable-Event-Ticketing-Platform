package com.example.demo.consumer;

import java.math.BigDecimal;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.example.demo.dto.PaymentReq;
import com.example.demo.dto.TicketsLockedEvent;
import com.example.demo.enums.PaymentMethod;
import com.example.demo.service.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class TicketsLockedConsumer {

    private final ObjectMapper objectMapper;
    private final PaymentService paymentService;

    public TicketsLockedConsumer(ObjectMapper objectMapper,
            PaymentService paymentService) {
        this.objectMapper = objectMapper;
        this.paymentService = paymentService;
    }

    @KafkaListener(topics = "ticket-events", groupId = "payment-service-group")
    public void consumeTicketsLocked(String message) {
        System.out.println("Received TICKETS_LOCKED event: " + message);
        try {
            TicketsLockedEvent event = objectMapper.readValue(message, TicketsLockedEvent.class);

            if ("TICKETS_LOCKED".equals(event.eventType()) && event.success()) {
                System.out.println(" Processing payment for orderId: " + event.orderId());
                if (paymentService instanceof com.example.demo.service.impl.PaymentServiceImpl) {
                    ((com.example.demo.service.impl.PaymentServiceImpl) paymentService)
                            .storeOrderTicketInfo(event.orderId(), event.stockId(), event.quantity());
                }

                PaymentReq paymentReq = new PaymentReq(
                        event.orderId(),
                        event.userId(),
                        new BigDecimal(event.totalAmount()),
                        event.currency(),
                        PaymentMethod.CREDIT_CARD,
                        "4111111111111111",
                        "123",
                        "12/25",
                        "Test User"
                );

                paymentService.createPayment(paymentReq);
                System.out.println("Payment request created for orderId: " + event.orderId());
            } else if ("TICKETS_LOCKED".equals(event.eventType()) && !event.success()) {
                System.err.println("Tickets lock failed for orderId: " + event.orderId()
                        + ", reason: " + event.failureReason());
            }
        } catch (Exception e) {
            System.err.println("Failed to process TICKETS_LOCKED event: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
