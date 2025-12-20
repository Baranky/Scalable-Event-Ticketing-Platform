package com.example.demo.service;

public interface PaymentEventHandlerService {
    
    void handlePaymentSuccess(String orderId);
    
    void handlePaymentFailed(String orderId);
}

