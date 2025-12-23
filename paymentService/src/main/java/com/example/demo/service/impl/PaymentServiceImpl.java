package com.example.demo.service.impl;

import com.example.demo.dto.OrderCompletedEvent;
import com.example.demo.dto.PaymentEvent;
import com.example.demo.dto.PaymentReq;
import com.example.demo.dto.PaymentResponse;
import com.example.demo.enums.PaymentStatus;
import com.example.demo.entity.Payment;
import com.example.demo.entity.PaymentOutbox;
import com.example.demo.repository.PaymentOutboxRepository;
import com.example.demo.repository.PaymentRepository;
import com.example.demo.service.PaymentService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;


@Service
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentOutboxRepository paymentOutboxRepository;
    private final ObjectMapper objectMapper;


    public PaymentServiceImpl(PaymentRepository paymentRepository,
                              PaymentOutboxRepository paymentOutboxRepository,
                              ObjectMapper objectMapper) {
        this.paymentRepository = paymentRepository;
        this.paymentOutboxRepository = paymentOutboxRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public PaymentResponse createPayment(PaymentReq request) {

        PaymentStatus status = simulateBankPayment(request);

        Payment payment = new Payment();
        payment.setOrderId(request.orderId());
        payment.setUserId(request.userId());
        payment.setAmount(request.amount());
        payment.setCurrency(request.currency());
        payment.setPaymentMethod(request.paymentMethod());
        payment.setStatus(status);
        payment.setExternalTransactionId(UUID.randomUUID().toString());
        payment.setMaskedCardNumber(maskCardNumber(request.cardNumber()));
        payment.setCardHolderName(request.cardHolderName());

        if (status == PaymentStatus.FAILED) {
            payment.setFailureReason("Simulated bank rejection");
        }

        Payment savedPayment = paymentRepository.save(payment);
        System.out.println("    Payment kaydı oluşturuldu: " + savedPayment.getId());

        saveToOutbox(savedPayment, getPaymentEventType(status));
        System.out.println("    Outbox kaydı oluşturuldu: " + getPaymentEventType(status));

        if (status == PaymentStatus.SUCCESS) {
            saveOrderCompletedToOutbox(savedPayment);
            System.out.println("  Outbox kaydı oluşturuldu: ORDER_COMPLETED");
        }


        return toResponse(savedPayment);
    }

    private PaymentStatus simulateBankPayment(PaymentReq request) {
        if (request.cardNumber() != null && request.cardNumber().startsWith("4000")) {
            return PaymentStatus.FAILED;
        }
        return PaymentStatus.SUCCESS;
    }


    private void saveToOutbox(Payment payment, String eventType) {
        PaymentOutbox outbox = new PaymentOutbox();
        outbox.setAggregateType("Payment");
        outbox.setAggregateId(payment.getId());
        outbox.setEventType(eventType);
        outbox.setTopic("payment-events");
        outbox.setPayload(buildPaymentEventPayload(payment, eventType));
        outbox.setProcessed(false);
        outbox.setRetryCount(0);

        paymentOutboxRepository.save(outbox);
    }


    private void saveOrderCompletedToOutbox(Payment payment) {
        PaymentOutbox outbox = new PaymentOutbox();
        outbox.setAggregateType("Order");
        outbox.setAggregateId(payment.getOrderId());
        outbox.setEventType("ORDER_COMPLETED");
        outbox.setTopic("order-events");
        outbox.setPayload(buildOrderCompletedPayload(payment));
        outbox.setProcessed(false);
        outbox.setRetryCount(0);

        paymentOutboxRepository.save(outbox);
    }

    private String getPaymentEventType(PaymentStatus status) {
        return status == PaymentStatus.SUCCESS ? "PAYMENT_SUCCESS" : "PAYMENT_FAILED";
    }

    private String buildPaymentEventPayload(Payment payment, String eventType) {
        try {
            return objectMapper.writeValueAsString( new PaymentEvent(
                    eventType,
                    payment.getId(),
                    payment.getOrderId(),
                    payment.getUserId(),
                    payment.getAmount().toString(),
                    payment.getCurrency(),
                    payment.getStatus().name(),
                    payment.getCreatedAt() != null ? payment.getCreatedAt().toString() : null
            ));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize payment event", e);
        }
    }

    private String buildOrderCompletedPayload(Payment payment) {
        try {
            return objectMapper.writeValueAsString(new OrderCompletedEvent(
                    "ORDER_COMPLETED",
                    payment.getOrderId(),
                    payment.getUserId(),
                    payment.getId()
            ));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize order completed event", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(String id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + id));
        return toResponse(payment);
    }

    private PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getOrderId(),
                payment.getUserId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getStatus(),
                payment.getPaymentMethod(),
                payment.getMaskedCardNumber(),
                payment.getCardHolderName(),
                payment.getCreatedAt()
        );
    }

    private String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return "****";
        }
        String last4 = cardNumber.substring(cardNumber.length() - 4);
        return "**** **** **** " + last4;
    }




}
