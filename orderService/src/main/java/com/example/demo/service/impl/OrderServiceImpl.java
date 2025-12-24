package com.example.demo.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.client.PaymentClient;
import com.example.demo.client.TicketClient;
import com.example.demo.dto.OrderCreateRequest;
import com.example.demo.dto.OrderItemDto;
import com.example.demo.dto.OrderResponse;
import com.example.demo.dto.OrderSagaRequest;
import com.example.demo.dto.SagaStatusResponse;
import com.example.demo.dto.TicketPurchaseRequest;
import com.example.demo.dto.TicketResponse;
import com.example.demo.dto.TicketStockResponse;
import com.example.demo.entity.Order;
import com.example.demo.entity.OrderItem;
import com.example.demo.entity.OrderSaga;
import com.example.demo.enums.OrderStatus;
import com.example.demo.repository.OrderItemRepository;
import com.example.demo.repository.OrderRepository;
import com.example.demo.repository.OrderSagaRepository;
import com.example.demo.saga.OrderSagaOrchestrator;
import com.example.demo.service.OrderService;
import com.example.demo.service.OutboxService;

@Service
public class OrderServiceImpl implements OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderSagaRepository sagaRepository;
    private final TicketClient ticketClient;
    private final PaymentClient paymentClient;
    private final OrderSagaOrchestrator sagaOrchestrator;
    private final OutboxService outboxService;

    public OrderServiceImpl(OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            OrderSagaRepository sagaRepository,
            TicketClient ticketClient,
            PaymentClient paymentClient,
            OrderSagaOrchestrator sagaOrchestrator,
            OutboxService outboxService) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.sagaRepository = sagaRepository;
        this.ticketClient = ticketClient;
        this.paymentClient = paymentClient;
        this.sagaOrchestrator = sagaOrchestrator;
        this.outboxService = outboxService;
    }

    @Override
    public OrderResponse createOrderWithSaga(OrderSagaRequest request) {
        return sagaOrchestrator.executeSaga(request);
    }

    @Override
    @Transactional
    public OrderResponse createOrder(OrderCreateRequest request) {
        Order existing = orderRepository.findByIdempotencyKey(request.idempotencyKey()).orElse(null);
        if (existing != null) {
            return toResponse(existing, orderItemRepository.findByOrder_Id(existing.getId()));
        }

        TicketStockResponse stock = loadAndValidateStock(request);
        Order savedOrder = createPendingOrder(request, stock);
        List<OrderItem> items = createOrderItems(savedOrder, request, stock);
        saveOrderCreatedOutbox(savedOrder, request, stock);

        log.info("Order created: orderId={}, stockId={}, quantity={}", savedOrder.getId(),
                request.stockId(), request.quantity());
        return toResponse(savedOrder, items);
    }

    @Transactional
    public OrderResponse completeOrder(String orderId) {
        Order order = loadOrderForCompletion(orderId);
        List<TicketResponse> tickets = purchaseTicketsForOrder(order);
        List<OrderItem> items = updateOrderItemsWithTickets(orderId, tickets);
        completeOrderAndOutbox(order, tickets.size());

        log.info("Order completed: orderId={}, ticketCount={}", orderId, tickets.size());
        return toResponse(order, items);
    }

    @Transactional
    public OrderResponse cancelOrder(String orderId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NoSuchElementException("Order not found: " + orderId));

        if (order.getStatus() == OrderStatus.COMPLETED) {
            throw new IllegalStateException("Cannot cancel a completed order. Use refund instead.");
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setCancellationReason(reason);
        orderRepository.save(order);

        outboxService.saveOrderCancelledEvent(order, reason);

        log.info("Order cancelled: orderId={}, reason={}", orderId, reason);
        log.info("ORDER_CANCELLED event saved to outbox for orderId={}", orderId);

        return toResponse(order, orderItemRepository.findByOrder_Id(orderId));
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(String id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Order not found with id: " + id));

        List<OrderItem> items = orderItemRepository.findByOrder_Id(order.getId());
        return toResponse(order, items);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByUser(String userId) {
        return orderRepository.findByUserId(userId).stream()
                .map(order -> toResponse(order, orderItemRepository.findByOrder_Id(order.getId())))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByStatus(OrderStatus status) {
        return orderRepository.findByStatus(status).stream()
                .map(order -> toResponse(order, orderItemRepository.findByOrder_Id(order.getId())))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public SagaStatusResponse getSagaStatus(String orderId) {
        OrderSaga saga = sagaRepository.findByOrderId(orderId).orElse(null);
        if (saga == null) {
            return null;
        }

        return new SagaStatusResponse(
                saga.getId(),
                saga.getOrderId(),
                saga.getStatus(),
                saga.getCurrentStep(),
                saga.getFailedStep(),
                saga.getErrorMessage(),
                saga.getCompletedSteps(),
                saga.getCreatedAt(),
                saga.getCompletedAt()
        );
    }

    private OrderResponse toResponse(Order order, List<OrderItem> items) {
        List<OrderItemDto> itemDtos = items.stream()
                .map(this::toItemDto)
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


    private TicketStockResponse loadAndValidateStock(OrderCreateRequest request) {
        try {
            TicketStockResponse stock = ticketClient.getStockById(request.stockId());
            log.info("Stock found for stockId={}, price={}, availableCount={}",
                    stock.id(), stock.price(), stock.availableCount());

            if (stock.availableCount() < request.quantity()) {
                throw new RuntimeException("Not enough tickets available. Requested: " + request.quantity()
                        + ", Available: " + stock.availableCount());
            }

            return stock;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to get stock info for stockId={}, error={}", request.stockId(), e.getMessage());
            throw new RuntimeException("Stock not found or unavailable: " + request.stockId());
        }
    }

    private Order createPendingOrder(OrderCreateRequest request, TicketStockResponse stock) {
        Order order = new Order();
        order.setUserId(request.userId());
        order.setStatus(OrderStatus.PENDING);
        order.setStockId(request.stockId());
        order.setQuantity(request.quantity());
        order.setIdempotencyKey(request.idempotencyKey());

        BigDecimal unitPrice = stock.price();
        BigDecimal totalAmount = unitPrice.multiply(BigDecimal.valueOf(request.quantity()));
        order.setTotalAmount(totalAmount);
        order.setCurrency(stock.currency());

        return orderRepository.save(order);
    }

    private List<OrderItem> createOrderItems(Order savedOrder, OrderCreateRequest request, TicketStockResponse stock) {
        List<OrderItem> items = new ArrayList<>();
        List<String> seatLabels = request.seatLabels();
        BigDecimal unitPrice = stock.price();

        for (int i = 0; i < request.quantity(); i++) {
            OrderItem item = new OrderItem();
            item.setOrder(savedOrder);
            item.setStockId(request.stockId());
            item.setEventId(stock.eventId());
            item.setPrice(unitPrice);

            if (seatLabels != null && i < seatLabels.size()) {
                item.setSeatLabel(seatLabels.get(i));
            }

            items.add(item);
        }

        return orderItemRepository.saveAll(items);
    }

    private void saveOrderCreatedOutbox(Order savedOrder, OrderCreateRequest request, TicketStockResponse stock) {
        outboxService.saveOrderCreatedEvent(savedOrder, request.stockId(), request.quantity(), stock.eventId());
        log.info("ORDER_CREATED event saved to outbox for orderId={}", savedOrder.getId());
    }

    private Order loadOrderForCompletion(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NoSuchElementException("Order not found: " + orderId));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("Order is not in PENDING status");
        }
        return order;
    }

    private List<TicketResponse> purchaseTicketsForOrder(Order order) {
        List<String> seatLabels = orderItemRepository.findByOrder_Id(order.getId()).stream()
                .map(OrderItem::getSeatLabel)
                .collect(Collectors.toList());

        return ticketClient.purchaseTickets(
                new TicketPurchaseRequest(
                        order.getUserId(),
                        order.getStockId(),
                        order.getQuantity(),
                        seatLabels
                )
        );
    }

    private List<OrderItem> updateOrderItemsWithTickets(String orderId, List<TicketResponse> tickets) {
        List<OrderItem> items = orderItemRepository.findByOrder_Id(orderId);
        for (int i = 0; i < items.size() && i < tickets.size(); i++) {
            OrderItem item = items.get(i);
            TicketResponse ticket = tickets.get(i);
            item.setTicketId(ticket.id());
            item.setQrCode(ticket.qrCode());
            item.setEventId(ticket.eventId());
            item.setSeatLabel(ticket.seatLabel());
        }
        return orderItemRepository.saveAll(items);
    }

    private void completeOrderAndOutbox(Order order, int ticketCount) {
        order.setStatus(OrderStatus.COMPLETED);
        orderRepository.save(order);

        outboxService.saveOrderCompletedEvent(order, ticketCount);
        log.info("ORDER_COMPLETED event saved to outbox for orderId={}", order.getId());
    }
    private OrderItemDto toItemDto(OrderItem item) {
        return new OrderItemDto(
                item.getStockId(),
                item.getTicketId(),
                item.getEventId(),
                item.getSeatLabel(),
                item.getQrCode(),
                item.getPrice()
        );
    }

}
