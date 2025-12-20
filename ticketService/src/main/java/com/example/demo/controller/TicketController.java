package com.example.demo.controller;

import com.example.demo.dto.TicketHistoryRes;
import com.example.demo.dto.TicketPurchaseReq;
import com.example.demo.dto.TicketRes;
import com.example.demo.service.TicketService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tickets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }


    @PostMapping("/purchase")
    public ResponseEntity<List<TicketRes>> purchaseTickets(@RequestBody TicketPurchaseReq request) {
        List<TicketRes> tickets = ticketService.purchaseTickets(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(tickets);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TicketRes> getTicketById(@PathVariable String id) {
        TicketRes res = ticketService.getTicketById(id);
        return ResponseEntity.ok(res);
    }


    @GetMapping("/by-event/{eventId}")
    public ResponseEntity<List<TicketRes>> getTicketsByEvent(@PathVariable String eventId) {
        List<TicketRes> list = ticketService.getTicketsByEventId(eventId);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/by-user/{userId}")
    public ResponseEntity<List<TicketRes>> getTicketsByUser(@PathVariable String userId) {
        List<TicketRes> list = ticketService.getTicketsByUserId(userId);
        return ResponseEntity.ok(list);
    }

    @PostMapping("/{id}/use")
    public ResponseEntity<TicketRes> useTicket(@PathVariable String id,
            @RequestParam String usedBy) {
        TicketRes updated = ticketService.useTicket(id, usedBy);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{id}/refund")
    public ResponseEntity<TicketRes> refundTicket(@PathVariable String id,
            @RequestParam String refundedBy,
            @RequestParam(required = false) String reason) {
        TicketRes refunded = ticketService.refundTicket(id, refundedBy, reason);
        return ResponseEntity.ok(refunded);
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<List<TicketHistoryRes>> getHistory(@PathVariable String id) {
        List<TicketHistoryRes> history = ticketService.getTicketHistory(id);
        return ResponseEntity.ok(history);
    }
}
