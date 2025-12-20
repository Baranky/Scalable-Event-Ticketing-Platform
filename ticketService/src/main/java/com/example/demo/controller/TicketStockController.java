package com.example.demo.controller;

import com.example.demo.dto.TicketStockRes;
import com.example.demo.model.TicketStock;
import com.example.demo.service.TicketStockService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/ticket-stocks")
public class TicketStockController {

    private final TicketStockService ticketStockService;
    private final com.example.demo.repository.TicketStockRepository ticketStockRepository;

    public TicketStockController(TicketStockService ticketStockService,
            com.example.demo.repository.TicketStockRepository ticketStockRepository) {
        this.ticketStockService = ticketStockService;
        this.ticketStockRepository = ticketStockRepository;
    }


    @GetMapping("/{id}")
    public ResponseEntity<TicketStockRes> getStockById(@PathVariable String id) {
        TicketStock stock = ticketStockRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Stock not found: " + id));
        return ResponseEntity.ok(mapToResponse(stock));
    }


    @GetMapping("/by-event/{eventId}")
    public ResponseEntity<List<TicketStockRes>> getStocksByEvent(@PathVariable String eventId) {
        List<TicketStock> stocks = ticketStockService.getStocksByEventId(eventId);
        List<TicketStockRes> response = stocks.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }


    @GetMapping("/by-event/{eventId}/price-category/{priceCategoryId}")
    public ResponseEntity<TicketStockRes> getStockByEventAndPriceCategory(
            @PathVariable String eventId,
            @PathVariable String priceCategoryId) {
        TicketStock stock = ticketStockService.getStockByEventAndPriceCategory(eventId, priceCategoryId)
                .orElseThrow(() -> new RuntimeException("Stock not found for event: " + eventId 
                        + ", priceCategory: " + priceCategoryId));
        return ResponseEntity.ok(mapToResponse(stock));
    }


    @PatchMapping("/{id}/lock")
    public ResponseEntity<Boolean> lockTickets(
            @PathVariable String id,
            @RequestParam int count,
            @RequestParam String orderId,
            @RequestParam(required = false) List<String> seatLabels) {
        boolean success = ticketStockService.lockTickets(id, count, orderId, seatLabels);
        return ResponseEntity.ok(success);
    }

    @PatchMapping("/{id}/unlock")
    public ResponseEntity<Boolean> unlockTickets(
            @PathVariable String id,
            @RequestParam int count,
            @RequestParam String orderId) {
        boolean success = ticketStockService.unlockTickets(id, count, orderId);
        return ResponseEntity.ok(success);
    }


    @PatchMapping("/{id}/confirm-sale")
    public ResponseEntity<Boolean> confirmSale(
            @PathVariable String id,
            @RequestParam int count,
            @RequestParam String orderId) {
        boolean success = ticketStockService.confirmSale(id, count, orderId);
        return ResponseEntity.ok(success);
    }


    @GetMapping("/{id}/redis-locked-count")
    public ResponseEntity<Integer> getRedisLockedCount(@PathVariable String id) {
        int count = ticketStockService.getRedisLockedCount(id);
        return ResponseEntity.ok(count);
    }

    private TicketStockRes mapToResponse(TicketStock stock) {
        return new TicketStockRes(
                stock.getId(),
                stock.getEventId(),
                stock.getVenueId(),
                stock.getSectionId(),
                stock.getPriceCategoryId(),
                stock.getPrice(),
                stock.getCurrency(),
                stock.getTotalCount(),
                stock.getAvailableCount(),
                stock.getSoldCount(),
                stock.getLockedCount(),
                stock.getCreatedAt(),
                stock.getUpdatedAt()
        );
    }
}

