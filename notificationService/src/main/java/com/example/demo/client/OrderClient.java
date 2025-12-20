package com.example.demo.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "order-service")
public interface OrderClient {

    @GetMapping("/api/orders/{id}")
    OrderResponse getOrderById(@PathVariable("id") String orderId);

    record OrderResponse(
            String id,
            String userId,
            String status,
            java.math.BigDecimal totalAmount,
            String currency,
            java.util.List<OrderItemDto> items
            ) {

    }

    record OrderItemDto(
            String ticketId,
            String eventId,
            java.math.BigDecimal price
            ) {

    }
}
