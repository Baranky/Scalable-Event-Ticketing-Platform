package com.example.demo.repository;

import com.example.demo.model.PaymentOutbox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface PaymentOutboxRepository extends JpaRepository<PaymentOutbox, String> {

    @Query("SELECT o FROM PaymentOutbox o WHERE o.processed = false AND o.retryCount < 5 ORDER BY o.createdAt ASC")
    List<PaymentOutbox> findUnprocessedOutboxEvents();

    @Query(value = "SELECT * FROM payment_outbox WHERE processed = false AND retry_count < 5 ORDER BY created_at ASC LIMIT :limit", nativeQuery = true)
    List<PaymentOutbox> findUnprocessedWithLimit(@Param("limit") int limit);


    @Query("SELECT o FROM PaymentOutbox o WHERE o.processed = false AND o.retryCount >= 5")
    List<PaymentOutbox> findDeadLetterEvents();


    @Modifying
    @Query("DELETE FROM PaymentOutbox o WHERE o.processed = true AND o.processedAt < :threshold")
    int deleteProcessedBefore(@Param("threshold") LocalDateTime threshold);

    List<PaymentOutbox> findByAggregateIdOrderByCreatedAtAsc(String aggregateId);

    long countByProcessedFalse();

    long countByProcessedTrue();

    @Query("SELECT COUNT(o) FROM PaymentOutbox o WHERE o.processed = false AND o.retryCount >= 5")
    long countDeadLetters();
}
