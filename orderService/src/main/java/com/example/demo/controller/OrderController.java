package com.example.demo.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.demo.dto.OrderCreateRequest;
import com.example.demo.dto.OrderResponse;
import com.example.demo.dto.OrderSagaRequest;
import com.example.demo.enums.OrderStatus;
import com.example.demo.saga.SagaException;
import com.example.demo.service.OrderService;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@RequestBody OrderCreateRequest request) {
        OrderResponse response = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/saga")
    public ResponseEntity<?> createOrderWithSaga(@RequestBody OrderSagaRequest request) {
        try {
            OrderResponse response = orderService.createOrderWithSaga(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (SagaException e) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(Map.of(
                            "error", "SAGA_FAILED",
                            "message", e.getMessage(),
                            "failedStep", e.getFailedStep() != null ? e.getFailedStep().name() : "UNKNOWN",
                            "compensated", true
                    ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "UNEXPECTED_ERROR",
                            "message", e.getMessage()
                    ));
        }
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<OrderResponse> completeOrder(@PathVariable String id) {
        OrderResponse response = orderService.completeOrder(id);
        return ResponseEntity.ok(response);
    }


    @PostMapping("/{id}/cancel")
    public ResponseEntity<OrderResponse> cancelOrder(
            @PathVariable String id,
            @RequestParam(required = false, defaultValue = "User cancelled") String reason) {
        OrderResponse response = orderService.cancelOrder(id, reason);
        return ResponseEntity.ok(response);
    }


    @GetMapping("/{id}/saga-status")
    public ResponseEntity<?> getSagaStatus(@PathVariable String id) {
        try {
            var status = orderService.getSagaStatus(id);
            if (status == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable String id) {
        return ResponseEntity.ok(orderService.getOrderById(id));
    }

    @GetMapping
    public ResponseEntity<List<OrderResponse>> getOrders(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) OrderStatus status
    ) {
        if (userId != null) {
            return ResponseEntity.ok(orderService.getOrdersByUser(userId));
        } else if (status != null) {
            return ResponseEntity.ok(orderService.getOrdersByStatus(status));
        }
        return ResponseEntity.noContent().build();
    }
}
