package com.example.demo.repository;

import com.example.demo.entity.OrderSaga;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrderSagaRepository extends JpaRepository<OrderSaga, String> {
    
    Optional<OrderSaga> findByOrderId(String orderId);
}

