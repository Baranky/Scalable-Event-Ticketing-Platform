package com.example.demo.repository;

import com.example.demo.entity.OrderOutbox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderOutboxRepository extends JpaRepository<OrderOutbox, String> {

    @Query(value = "SELECT * FROM order_outbox WHERE processed = false AND retry_count < 5 ORDER BY created_at ASC LIMIT :limit", nativeQuery = true)
    List<OrderOutbox> findUnprocessedWithLimit(@Param("limit") int limit);

    @Query("SELECT o FROM OrderOutbox o WHERE o.processed = false AND o.retryCount >= 5")
    List<OrderOutbox> findDeadLetterEvents();

    @Modifying
    @Query("DELETE FROM OrderOutbox o WHERE o.processed = true AND o.processedAt < :threshold")
    int deleteProcessedBefore(@Param("threshold") LocalDateTime threshold);


    long countByProcessedFalse();

    long countByProcessedTrue();

    @Query("SELECT COUNT(o) FROM OrderOutbox o WHERE o.processed = false AND o.retryCount >= 5")
    long countDeadLetters();
}
