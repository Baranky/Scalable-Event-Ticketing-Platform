package com.example.demo.consumer;

import com.example.demo.dto.PaymentEvent;
import com.example.demo.service.PaymentEventHandlerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventConsumer {

    private final ObjectMapper objectMapper;
    private final PaymentEventHandlerService paymentEventHandlerService;

    public PaymentEventConsumer(ObjectMapper objectMapper,
                                PaymentEventHandlerService paymentEventHandlerService) {
        this.objectMapper = objectMapper;
        this.paymentEventHandlerService = paymentEventHandlerService;
    }

    @KafkaListener(topics = "payment-events", groupId = "ticket-service-group")
    public void consumePaymentEvent(String message) {
        System.out.println("📨 Received payment event: " + message);
        try {
            PaymentEvent event = objectMapper.readValue(message, PaymentEvent.class);
            
            if ("PAYMENT_SUCCESS".equals(event.eventType())) {
                paymentEventHandlerService.handlePaymentSuccess(event);
            } else if ("PAYMENT_FAILED".equals(event.eventType())) {
                paymentEventHandlerService.handlePaymentFailed(event.orderId());
            }
        } catch (Exception e) {
            System.err.println("❌ Failed to process payment event: " + e.getMessage());
            e.printStackTrace();
        }
    }


}

