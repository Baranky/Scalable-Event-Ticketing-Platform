package com.example.demo.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dto.TicketHistoryRes;
import com.example.demo.dto.TicketPurchaseReq;
import com.example.demo.dto.TicketRes;
import com.example.demo.enums.TicketStatus;
import com.example.demo.model.Ticket;
import com.example.demo.model.TicketStock;
import com.example.demo.repository.TicketRepository;
import com.example.demo.service.TicketHistoryService;
import com.example.demo.service.TicketService;
import com.example.demo.service.TicketStockService;

@Service
public class TicketServiceImpl implements TicketService {

    private final TicketRepository ticketRepository;
    private final TicketStockService ticketStockService;
    private final TicketHistoryService ticketHistoryService;

    public TicketServiceImpl(TicketRepository ticketRepository,
            TicketStockService ticketStockService,
            TicketHistoryService ticketHistoryService) {
        this.ticketRepository = ticketRepository;
        this.ticketStockService = ticketStockService;
        this.ticketHistoryService = ticketHistoryService;
    }

    @Override
    @Transactional
    public List<TicketRes> purchaseTickets(TicketPurchaseReq request) {
        TicketStock stock = ticketStockService.getStockEntityByIdWithLock(request.stockId())
                .orElseThrow(() -> new RuntimeException("Stock not found: " + request.stockId()));

        if (stock.getAvailableCount() < request.quantity()) {
            throw new RuntimeException("Not enough tickets available. Requested: " + request.quantity()
                    + ", Available: " + stock.getAvailableCount());
        }

        ticketStockService.decrementAvailableAndIncrementSold(request.stockId(), request.quantity());

        List<Ticket> tickets = new ArrayList<>();
        for (int i = 0; i < request.quantity(); i++) {
            Ticket ticket = new Ticket();
            ticket.setEventId(stock.getEventId());
            ticket.setVenueId(stock.getVenueId());
            ticket.setUserId(request.userId());
            ticket.setSectionId(stock.getSectionId() != null ? stock.getSectionId() : "general");
            ticket.setPriceCategoryId(stock.getPriceCategoryId());
            ticket.setPurchasePrice(stock.getPrice());
            ticket.setCurrency(stock.getCurrency());
            ticket.setStatus(TicketStatus.SOLD);
            ticket.setQrCode(generateQRCode());
            ticket.setPurchasedAt(LocalDateTime.now());

            if (request.seatLabels() != null && i < request.seatLabels().size()) {
                ticket.setSeatLabel(request.seatLabels().get(i));
            } else {
                ticket.setSeatLabel("GA-" + (stock.getSoldCount() - request.quantity() + i + 1)); // General Admission
            }

            tickets.add(ticket);
        }

        List<Ticket> savedTickets = ticketRepository.saveAll(tickets);

        for (Ticket ticket : savedTickets) {
            ticketHistoryService.recordStatusChange(ticket, null, TicketStatus.SOLD,
                    request.userId(), "User Purchase");
        }

        System.out.println("Created " + savedTickets.size() + " tickets for user: " + request.userId()
                + ", stock: " + request.stockId());

        return savedTickets.stream()
                .map(this::mapToTicketRes)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public TicketRes getTicketById(String id) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ticket not found: " + id));
        return mapToTicketRes(ticket);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketRes> getTicketsByEventId(String eventId) {
        return ticketRepository.findByEventId(eventId)
                .stream()
                .map(this::mapToTicketRes)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketRes> getTicketsByUserId(String userId) {
        return ticketRepository.findByUserId(userId)
                .stream()
                .map(this::mapToTicketRes)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public TicketRes useTicket(String ticketId, String usedBy) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found: " + ticketId));

        if (ticket.getStatus() != TicketStatus.SOLD) {
            throw new RuntimeException("Ticket cannot be used. Current status: " + ticket.getStatus());
        }

        TicketStatus previousStatus = ticket.getStatus();
        ticket.setStatus(TicketStatus.USED);
        ticket.setUsedAt(LocalDateTime.now());

        Ticket saved = ticketRepository.save(ticket);

        ticketHistoryService.recordStatusChange(saved, previousStatus, TicketStatus.USED,
                usedBy, "QR Code Scanned");

        System.out.println("Ticket used: " + ticketId);

        return mapToTicketRes(saved);
    }

    @Override
    @Transactional
    public TicketRes refundTicket(String ticketId, String refundedBy, String reason) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found: " + ticketId));

        if (ticket.getStatus() == TicketStatus.USED) {
            throw new RuntimeException("Cannot refund a used ticket");
        }
        if (ticket.getStatus() == TicketStatus.REFUNDED) {
            throw new RuntimeException("Ticket already refunded");
        }

        TicketStatus previousStatus = ticket.getStatus();
        ticket.setStatus(TicketStatus.REFUNDED);
        ticket.setRefundedAt(LocalDateTime.now());

        Ticket saved = ticketRepository.save(ticket);

        ticketStockService.incrementAvailableAndDecrementSold(
                ticket.getEventId(), ticket.getPriceCategoryId());

        ticketHistoryService.recordStatusChange(saved, previousStatus, TicketStatus.REFUNDED,
                refundedBy, reason != null ? reason : "Refund Request");

        System.out.println("Ticket refunded: " + ticketId);

        return mapToTicketRes(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketHistoryRes> getTicketHistory(String ticketId) {
        return ticketHistoryService.getHistoryByTicketId(ticketId);
    }

    private String generateQRCode() {
        return "QR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private TicketRes mapToTicketRes(Ticket ticket) {
        return new TicketRes(
                ticket.getId(),
                ticket.getEventId(),
                ticket.getVenueId(),
                ticket.getUserId(),
                ticket.getSectionId(),
                ticket.getSeatLabel(),
                ticket.getPriceCategoryId(),
                ticket.getStatus(),
                ticket.getPurchasePrice(),
                ticket.getCurrency(),
                ticket.getQrCode(),
                ticket.getPurchasedAt(),
                ticket.getUsedAt(),
                ticket.getRefundedAt(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt()
        );
    }
}
