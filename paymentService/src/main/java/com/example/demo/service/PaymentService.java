package com.example.demo.service;

import com.example.demo.dto.PaymentReq;
import com.example.demo.dto.PaymentResponse;

public interface PaymentService {

    PaymentResponse createPayment(PaymentReq request);

    PaymentResponse getPaymentById(String id);
}


