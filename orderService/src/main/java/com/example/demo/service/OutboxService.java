package com.example.demo.service;

import com.example.demo.entity.Order;
import com.example.demo.entity.OrderOutbox;
import com.example.demo.repository.OrderOutboxRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;


@Service
public class OutboxService {

    private static final String ORDER_EVENTS_TOPIC = "order-events";
    private static final Logger log = LoggerFactory.getLogger(OutboxService.class);

    private final OrderOutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public OutboxService(OrderOutboxRepository outboxRepository, ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    public void saveOrderCreatedEvent(Order order, String stockId, int quantity, String eventId) {
        OrderOutbox outbox = createOutbox(order.getId(), "ORDER_CREATED");
        
        outbox.setPayload(toJson(Map.of(
                "eventType", "ORDER_CREATED",
                "orderId", order.getId(),
                "userId", order.getUserId(),
                "stockId", stockId,
                "quantity", quantity,
                "eventId", eventId != null ? eventId : "",
                "totalAmount", order.getTotalAmount().toString(),
                "currency", order.getCurrency(),
                "status", order.getStatus().name()
        )));
        
        outboxRepository.save(outbox);
        log.info("Outbox ORDER_CREATED saved for orderId={}", order.getId());
    }


    public void saveOrderCompletedEvent(Order order, int ticketCount) {
        OrderOutbox outbox = createOutbox(order.getId(), "ORDER_COMPLETED");
        
        outbox.setPayload(toJson(Map.of(
                "eventType", "ORDER_COMPLETED",
                "orderId", order.getId(),
                "userId", order.getUserId(),
                "ticketCount", ticketCount,
                "totalAmount", order.getTotalAmount().toString(),
                "currency", order.getCurrency()
        )));
        
        outboxRepository.save(outbox);
        log.info("Outbox ORDER_COMPLETED saved for orderId={}", order.getId());
    }

    public void saveOrderCancelledEvent(Order order, String reason) {
        OrderOutbox outbox = createOutbox(order.getId(), "ORDER_CANCELLED");
        
        outbox.setPayload(toJson(Map.of(
                "eventType", "ORDER_CANCELLED",
                "orderId", order.getId(),
                "userId", order.getUserId(),
                "reason", reason != null ? reason : "Unknown",
                "stockId", order.getStockId(),
                "quantity", order.getQuantity()
        )));
        
        outboxRepository.save(outbox);
        log.info("Outbox ORDER_CANCELLED saved for orderId={}", order.getId());
    }


    public void saveOrderPaymentFailedEvent(Order order, String failureReason) {
        OrderOutbox outbox = createOutbox(order.getId(), "ORDER_PAYMENT_FAILED");
        
        outbox.setPayload(toJson(Map.of(
                "eventType", "ORDER_PAYMENT_FAILED",
                "orderId", order.getId(),
                "userId", order.getUserId(),
                "reason", failureReason != null ? failureReason : "Payment failed",
                "stockId", order.getStockId(),
                "quantity", order.getQuantity()
        )));
        
        outboxRepository.save(outbox);
        log.info("Outbox ORDER_PAYMENT_FAILED saved for orderId={}", order.getId());
    }


    private OrderOutbox createOutbox(String orderId, String eventType) {
        OrderOutbox outbox = new OrderOutbox();
        outbox.setAggregateType("Order");
        outbox.setAggregateId(orderId);
        outbox.setEventType(eventType);
        outbox.setTopic(ORDER_EVENTS_TOPIC);
        outbox.setProcessed(false);
        outbox.setRetryCount(0);
        return outbox;
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize outbox payload", e);
        }
    }
}

