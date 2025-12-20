package com.example.demo.saga;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.client.PaymentClient;
import com.example.demo.client.TicketClient;
import com.example.demo.dto.OrderItemDto;
import com.example.demo.dto.OrderResponse;
import com.example.demo.dto.OrderSagaRequest;
import com.example.demo.enums.OrderStatus;
import com.example.demo.model.Order;
import com.example.demo.model.OrderItem;
import com.example.demo.model.OrderOutbox;
import com.example.demo.model.OrderSaga;
import com.example.demo.repository.OrderItemRepository;
import com.example.demo.repository.OrderOutboxRepository;
import com.example.demo.repository.OrderRepository;
import com.example.demo.repository.OrderSagaRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class OrderSagaOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(OrderSagaOrchestrator.class);

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderOutboxRepository orderOutboxRepository;
    private final OrderSagaRepository sagaRepository;
    private final TicketClient ticketClient;
    private final PaymentClient paymentClient;
    private final ObjectMapper objectMapper;

    public OrderSagaOrchestrator(
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            OrderOutboxRepository orderOutboxRepository,
            OrderSagaRepository sagaRepository,
            TicketClient ticketClient,
            PaymentClient paymentClient,
            ObjectMapper objectMapper) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.orderOutboxRepository = orderOutboxRepository;
        this.sagaRepository = sagaRepository;
        this.ticketClient = ticketClient;
        this.paymentClient = paymentClient;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public OrderResponse executeSaga(OrderSagaRequest request) {
        Order existingOrder = orderRepository.findByIdempotencyKey(request.idempotencyKey()).orElse(null);
        if (existingOrder != null) {
            log.warn("Order already exists for idempotencyKey={}, orderId={}", request.idempotencyKey(), existingOrder.getId());
            return toResponse(existingOrder, orderItemRepository.findByOrder_Id(existingOrder.getId()));
        }

        TicketClient.TicketStockResponse stock;
        try {
            stock = ticketClient.getStockById(request.stockId());
            log.info("Stock found for stockId={}, price={}, currency={}, availableCount={}",
                    stock.id(), stock.price(), stock.currency(), stock.availableCount());
        } catch (Exception e) {
            throw new SagaException("Stok bulunamadı: " + request.stockId(), null);
        }

        if (stock.availableCount() < request.quantity()) {
            throw new SagaException("Yetersiz stok. İstenen: " + request.quantity()
                    + ", Mevcut: " + stock.availableCount(), null);
        }

        Order order = createPendingOrder(request, stock);
        List<OrderItem> items = createOrderItems(order, request, stock);

        OrderSaga saga = createSaga(order.getId());

        List<SagaStep> completedSteps = new ArrayList<>();

        try {
            updateSagaStep(saga, SagaStep.LOCK_TICKETS);

            boolean locked = ticketClient.lockTickets(
                    request.stockId(),
                    request.quantity(),
                    order.getId(),
                    request.seatLabels()
            );

            if (!locked) {
                throw new SagaException("Biletler kilitlenemedi", SagaStep.LOCK_TICKETS);
            }

            completedSteps.add(SagaStep.LOCK_TICKETS);

            updateSagaStep(saga, SagaStep.PROCESS_PAYMENT);
            order.setStatus(OrderStatus.PAYMENT_PROCESSING);
            orderRepository.save(order);

            PaymentClient.PaymentResponse paymentResponse;
            try {
                paymentResponse = paymentClient.createPayment(new PaymentClient.PaymentRequest(
                        order.getId(),
                        order.getUserId(),
                        order.getTotalAmount(),
                        order.getCurrency(),
                        request.paymentMethod(),
                        request.cardNumber(),
                        request.cvv(),
                        request.expireDate(),
                        request.cardHolderName()
                ));

                if (!"SUCCESS".equals(paymentResponse.status())) {
                    throw new SagaException("Ödeme başarısız: " + paymentResponse.status(), SagaStep.PROCESS_PAYMENT);
                }
            } catch (SagaException e) {
                throw e;
            } catch (Exception e) {
                throw new SagaException("Ödeme işlenirken hata: " + e.getMessage(), SagaStep.PROCESS_PAYMENT);
            }

            completedSteps.add(SagaStep.PROCESS_PAYMENT);

            updateSagaStep(saga, SagaStep.CONFIRM_SALE);
            order.setStatus(OrderStatus.PAID);
            orderRepository.save(order);

            boolean confirmed = ticketClient.confirmSale(
                    request.stockId(),
                    request.quantity(),
                    order.getId()
            );

            if (!confirmed) {
                throw new SagaException("Satış onaylanamadı", SagaStep.CONFIRM_SALE);
            }

            completedSteps.add(SagaStep.CONFIRM_SALE);

            updateSagaStep(saga, SagaStep.CREATE_TICKETS);

            List<String> seatLabels = items.stream()
                    .map(OrderItem::getSeatLabel)
                    .collect(Collectors.toList());

            List<TicketClient.TicketResponse> tickets = ticketClient.purchaseTickets(
                    new TicketClient.TicketPurchaseRequest(
                            order.getUserId(),
                            order.getStockId(),
                            order.getQuantity(),
                            seatLabels
                    )
            );

            for (int i = 0; i < items.size() && i < tickets.size(); i++) {
                OrderItem item = items.get(i);
                TicketClient.TicketResponse ticket = tickets.get(i);
                item.setTicketId(ticket.id());
                item.setQrCode(ticket.qrCode());
                item.setEventId(ticket.eventId());
                item.setSeatLabel(ticket.seatLabel());
            }
            orderItemRepository.saveAll(items);

            completedSteps.add(SagaStep.CREATE_TICKETS);
            log.info("{} tickets created for orderId={}", tickets.size(), order.getId());

            updateSagaStep(saga, SagaStep.COMPLETE_ORDER);
            log.info("Step COMPLETE_ORDER started for orderId={}", order.getId());

            order.setStatus(OrderStatus.COMPLETED);
            orderRepository.save(order);

            createOutboxEvent(order, "ORDER_COMPLETED");

            completedSteps.add(SagaStep.COMPLETE_ORDER);
            log.info("Order completed successfully, orderId={}", order.getId());

            saga.setStatus(SagaStatus.COMPLETED);
            saga.setCompletedSteps(stepsToJson(completedSteps));
            saga.setCompletedAt(LocalDateTime.now());
            sagaRepository.save(saga);

            return toResponse(order, items);

        } catch (SagaException e) {
            log.error("Saga failed for orderId={}, failedStep={}, message={}",
                    order.getId(), e.getFailedStep(), e.getMessage());
            log.info("Starting compensation for orderId={}", order.getId());

            compensate(order, saga, completedSteps, e);

            throw e;
        }
    }

    private void compensate(Order order, OrderSaga saga, List<SagaStep> completedSteps, SagaException error) {
        saga.setStatus(SagaStatus.COMPENSATING);
        saga.setFailedStep(error.getFailedStep());
        saga.setErrorMessage(error.getMessage());
        sagaRepository.save(saga);

        for (int i = completedSteps.size() - 1; i >= 0; i--) {
            SagaStep step = completedSteps.get(i);
            System.out.println("\n🔙 Compensating: " + step.getDescription() + " → " + step.getCompensationDescription());

            try {
                switch (step) {
                    case LOCK_TICKETS ->
                        compensateLockTickets(order);
                    case PROCESS_PAYMENT ->
                        compensatePayment(order);
                    case CONFIRM_SALE ->
                        compensateConfirmSale(order);
                    case CREATE_TICKETS ->
                        compensateCreateTickets(order);
                    case COMPLETE_ORDER ->
                        compensateCompleteOrder(order);
                }
            } catch (Exception ce) {
            }
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setCancellationReason(error.getMessage());
        orderRepository.save(order);

        saga.setStatus(SagaStatus.COMPENSATED);
        saga.setCompletedAt(LocalDateTime.now());
        sagaRepository.save(saga);

    }

    private void compensateLockTickets(Order order) {
        // Bilet kilidini aç
        ticketClient.unlockTickets(order.getStockId(), order.getQuantity(), order.getId());
        System.out.println("   🔓 Bilet kilidi açıldı: " + order.getQuantity() + " adet");
    }

    private void compensatePayment(Order order) {

    }

    private void compensateConfirmSale(Order order) {

    }

    private void compensateCreateTickets(Order order) {

    }

    private void compensateCompleteOrder(Order order) {

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
    }

    private Order createPendingOrder(OrderSagaRequest request, TicketClient.TicketStockResponse stock) {
        Order order = new Order();
        order.setUserId(request.userId());
        order.setStatus(OrderStatus.PENDING);
        order.setStockId(request.stockId());
        order.setQuantity(request.quantity());
        order.setIdempotencyKey(request.idempotencyKey());

        BigDecimal totalAmount = stock.price().multiply(BigDecimal.valueOf(request.quantity()));
        order.setTotalAmount(totalAmount);
        order.setCurrency(stock.currency());

        return orderRepository.save(order);
    }

    private List<OrderItem> createOrderItems(Order order, OrderSagaRequest request, TicketClient.TicketStockResponse stock) {
        List<OrderItem> items = new ArrayList<>();
        List<String> seatLabels = request.seatLabels();

        for (int i = 0; i < request.quantity(); i++) {
            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setStockId(request.stockId());
            item.setEventId(stock.eventId());
            item.setPrice(stock.price());

            if (seatLabels != null && i < seatLabels.size()) {
                item.setSeatLabel(seatLabels.get(i));
            }

            items.add(item);
        }

        return orderItemRepository.saveAll(items);
    }

    private OrderSaga createSaga(String orderId) {
        OrderSaga saga = new OrderSaga();
        saga.setOrderId(orderId);
        saga.setStatus(SagaStatus.STARTED);
        return sagaRepository.save(saga);
    }

    private void updateSagaStep(OrderSaga saga, SagaStep step) {
        saga.setCurrentStep(step);
        saga.setStatus(SagaStatus.IN_PROGRESS);
        sagaRepository.save(saga);
    }

    private void createOutboxEvent(Order order, String eventType) {
        OrderOutbox outbox = new OrderOutbox();
        outbox.setAggregateId(order.getId());
        outbox.setEventType(eventType);
        outbox.setPayload("{\"orderId\":\"" + order.getId() + "\",\"userId\":\"" + order.getUserId() + "\"}");
        outbox.setProcessed(false);
        orderOutboxRepository.save(outbox);
    }

    private String stepsToJson(List<SagaStep> steps) {
        try {
            return objectMapper.writeValueAsString(steps);
        } catch (JsonProcessingException e) {
            return steps.toString();
        }
    }

    private OrderResponse toResponse(Order order, List<OrderItem> items) {
        List<OrderItemDto> itemDtos = items.stream()
                .map(item -> new OrderItemDto(
                item.getStockId(),
                item.getTicketId(),
                item.getEventId(),
                item.getSeatLabel(),
                item.getQrCode(),
                item.getPrice()
        ))
                .collect(Collectors.toList());

        return new OrderResponse(
                order.getId(),
                order.getUserId(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getCurrency(),
                order.getStockId(),
                order.getQuantity(),
                order.getIdempotencyKey(),
                order.getCancellationReason(),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                itemDtos
        );
    }
}
