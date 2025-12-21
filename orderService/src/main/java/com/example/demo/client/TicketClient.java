package com.example.demo.client;

import com.example.demo.dto.TicketPurchaseRequest;
import com.example.demo.dto.TicketResponse;
import com.example.demo.dto.TicketStockResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

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
}
