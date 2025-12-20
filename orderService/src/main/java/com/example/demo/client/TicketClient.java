package com.example.demo.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;


@FeignClient(name = "ticketService")
public interface TicketClient {


    @GetMapping("/ticket-stocks/{id}")
    TicketStockResponse getStockById(@PathVariable("id") String stockId);


    @GetMapping("/ticket-stocks/by-event/{eventId}/price-category/{priceCategoryId}")
    TicketStockResponse getStockByEventAndPriceCategory(
            @PathVariable("eventId") String eventId,
            @PathVariable("priceCategoryId") String priceCategoryId);

    @GetMapping("/ticket-stocks/by-event/{eventId}")
    List<TicketStockResponse> getStocksByEvent(@PathVariable("eventId") String eventId);


    @PatchMapping("/ticket-stocks/{id}/lock")
    Boolean lockTickets(
            @PathVariable("id") String stockId,
            @RequestParam("count") int count,
            @RequestParam("orderId") String orderId,
            @RequestParam(value = "seatLabels", required = false) List<String> seatLabels);


    @PatchMapping("/ticket-stocks/{id}/unlock")
    Boolean unlockTickets(
            @PathVariable("id") String stockId,
            @RequestParam("count") int count,
            @RequestParam("orderId") String orderId);


    @PatchMapping("/ticket-stocks/{id}/confirm-sale")
    Boolean confirmSale(
            @PathVariable("id") String stockId,
            @RequestParam("count") int count,
            @RequestParam("orderId") String orderId);


    @GetMapping("/ticket-stocks/{id}/redis-locked-count")
    Integer getRedisLockedCount(@PathVariable("id") String stockId);


    @PostMapping("/tickets/purchase")
    List<TicketResponse> purchaseTickets(@RequestBody TicketPurchaseRequest request);


    @PostMapping("/tickets/{id}/use")
    TicketResponse useTicket(@PathVariable("id") String ticketId, @RequestParam("usedBy") String usedBy);


    @PostMapping("/tickets/{id}/refund")
    TicketResponse refundTicket(@PathVariable("id") String ticketId,
                                 @RequestParam("refundedBy") String refundedBy,
                                 @RequestParam(value = "reason", required = false) String reason);


    record TicketStockResponse(
            String id,
            String eventId,
            String venueId,
            String sectionId,
            String priceCategoryId,
            BigDecimal price,
            String currency,
            int totalCount,
            int availableCount,
            int soldCount,
            int lockedCount
    ) {}

    record TicketPurchaseRequest(
            String userId,
            String stockId,
            int quantity,
            List<String> seatLabels
    ) {}

    record TicketResponse(
            String id,
            String eventId,
            String venueId,
            String userId,
            String sectionId,
            String seatLabel,
            String priceCategoryId,
            String status,
            BigDecimal purchasePrice,
            String currency,
            String qrCode,
            LocalDateTime purchasedAt
    ) {}
}
