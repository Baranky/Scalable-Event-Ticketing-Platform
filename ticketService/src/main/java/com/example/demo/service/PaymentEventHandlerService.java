package com.example.demo.service;

import com.example.demo.dto.PaymentEvent;
import java.util.List;

public interface PaymentEventHandlerService {
    
    void handlePaymentSuccess(PaymentEvent event);
    
    void handlePaymentFailed(String orderId);
    
    void storeOrderSeatLabels(String orderId, List<String> seatLabels);
}

