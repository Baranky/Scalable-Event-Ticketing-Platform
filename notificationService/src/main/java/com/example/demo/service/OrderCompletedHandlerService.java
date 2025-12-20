package com.example.demo.service;

public interface OrderCompletedHandlerService {

    void handleOrderCompleted(String orderId, String userId, String paymentId);
}
