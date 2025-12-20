package com.example.demo.service;

import com.example.demo.consumer.EventCreatedConsumer.PriceCategoryDetail;
import com.example.demo.model.TicketStock;

import java.util.List;
import java.util.Optional;

public interface TicketStockService {


    void createTicketStockForEvent(String eventId, String venueId, List<PriceCategoryDetail> priceCategories);


    Optional<TicketStock> getStockById(String stockId);


    List<TicketStock> getStocksByEventId(String eventId);


    Optional<TicketStock> getStockByEventAndPriceCategory(String eventId, String priceCategoryId);

    boolean lockTickets(String stockId, int count, String orderId, List<String> seatLabels);

    boolean confirmSale(String stockId, int count, String orderId);

    boolean unlockTickets(String stockId, int count, String orderId);


    int getRedisLockedCount(String stockId);

    TicketStock save(TicketStock stock);
}
