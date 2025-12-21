package com.example.demo.client;

import com.example.demo.dto.PaymentRequest;
import com.example.demo.dto.PaymentResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@FeignClient(name = "paymentService")
public interface PaymentClient {

    @PostMapping("/api/payments")
    PaymentResponse createPayment(@RequestBody PaymentRequest request);

}


