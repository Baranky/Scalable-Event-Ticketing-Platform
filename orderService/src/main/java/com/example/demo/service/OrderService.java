package com.example.demo.service;

import com.example.demo.dto.OrderCreateRequest;
import com.example.demo.dto.OrderResponse;
import com.example.demo.dto.OrderSagaRequest;
import com.example.demo.dto.SagaStatusResponse;
import com.example.demo.enums.OrderStatus;

import java.util.List;

public interface OrderService {

    OrderResponse createOrder(OrderCreateRequest request);

    OrderResponse createOrderWithSaga(OrderSagaRequest request);

    OrderResponse completeOrder(String orderId);

    OrderResponse cancelOrder(String orderId, String reason);

    OrderResponse getOrderById(String id);

    List<OrderResponse> getOrdersByUser(String userId);

    List<OrderResponse> getOrdersByStatus(OrderStatus status);

    SagaStatusResponse getSagaStatus(String orderId);
}
