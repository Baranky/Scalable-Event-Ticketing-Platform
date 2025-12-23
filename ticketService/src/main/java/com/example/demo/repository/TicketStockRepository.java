package com.example.demo.repository;

import com.example.demo.entity.TicketStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

@Repository
public interface TicketStockRepository extends JpaRepository<TicketStock, String> {

    List<TicketStock> findByEventId(String eventId);

    Optional<TicketStock> findByEventIdAndPriceCategoryId(String eventId, String priceCategoryId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ts FROM TicketStock ts WHERE ts.id = :id")
    Optional<TicketStock> findByIdWithLock(@Param("id") String id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ts FROM TicketStock ts WHERE ts.eventId = :eventId AND ts.priceCategoryId = :priceCategoryId")
    Optional<TicketStock> findByEventIdAndPriceCategoryIdWithLock(
            @Param("eventId") String eventId,
            @Param("priceCategoryId") String priceCategoryId);

    @Query("SELECT ts FROM TicketStock ts WHERE ts.eventId = :eventId AND ts.availableCount > 0")
    List<TicketStock> findAvailableStocksByEventId(@Param("eventId") String eventId);

    boolean existsByEventIdAndPriceCategoryId(String eventId, String priceCategoryId);

    List<TicketStock> findByLockedCountGreaterThan(int count);
}
