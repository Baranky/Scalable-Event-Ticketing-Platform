package com.example.demo.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.TicketStockRes;
import com.example.demo.service.TicketStockService;

@RestController
@RequestMapping("/ticket-stocks")
public class TicketStockController {

    private final TicketStockService ticketStockService;

    public TicketStockController(TicketStockService ticketStockService) {
        this.ticketStockService = ticketStockService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<TicketStockRes> getStockById(@PathVariable String id) {
        TicketStockRes stock = ticketStockService.getStockById(id)
                .orElseThrow(() -> new RuntimeException("Stock not found: " + id));
        return ResponseEntity.ok(stock);
    }

    @GetMapping("/by-event/{eventId}")
    public ResponseEntity<List<TicketStockRes>> getStocksByEvent(@PathVariable String eventId) {
        List<TicketStockRes> response = ticketStockService.getStocksByEventId(eventId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/by-event/{eventId}/price-category/{priceCategoryId}")
    public ResponseEntity<TicketStockRes> getStockByEventAndPriceCategory(
            @PathVariable String eventId,
            @PathVariable String priceCategoryId) {
        TicketStockRes stock = ticketStockService.getStockByEventAndPriceCategory(eventId, priceCategoryId)
                .orElseThrow(() -> new RuntimeException("Stock not found for event: " + eventId
                + ", priceCategory: " + priceCategoryId));
        return ResponseEntity.ok(stock);
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
}
