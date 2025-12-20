package com.example.demo.repository;

import com.example.demo.model.OrderSaga;
import com.example.demo.saga.SagaStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderSagaRepository extends JpaRepository<OrderSaga, String> {
    
    Optional<OrderSaga> findByOrderId(String orderId);
    
    List<OrderSaga> findByStatus(SagaStatus status);

    List<OrderSaga> findByStatusAndUpdatedAtBefore(SagaStatus status, LocalDateTime threshold);
}

