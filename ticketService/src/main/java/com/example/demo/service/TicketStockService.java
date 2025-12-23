package com.example.demo.service;

import java.util.List;
import java.util.Optional;

import com.example.demo.dto.PriceCategoryDetail;
import com.example.demo.dto.TicketStockRes;
import com.example.demo.entity.TicketStock;

public interface TicketStockService {

    void createTicketStockForEvent(String eventId, String venueId, List<PriceCategoryDetail> priceCategories);

    Optional<TicketStockRes> getStockById(String stockId);

    List<TicketStockRes> getStocksByEventId(String eventId);

    Optional<TicketStockRes> getStockByEventAndPriceCategory(String eventId, String priceCategoryId);

    boolean lockTickets(String stockId, int count, String orderId, List<String> seatLabels);

    boolean confirmSale(String stockId, int count, String orderId);

    boolean unlockTickets(String stockId, int count, String orderId);

    int getRedisLockedCount(String stockId);

    Optional<TicketStock> getStockEntityByIdWithLock(String stockId);

    void decrementAvailableAndIncrementSold(String stockId, int quantity);

    void incrementAvailableAndDecrementSold(String eventId, String priceCategoryId);
}
