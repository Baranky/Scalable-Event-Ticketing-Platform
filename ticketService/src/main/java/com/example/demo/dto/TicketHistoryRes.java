package com.example.demo.dto;

import com.example.demo.enums.TicketStatus;

import java.time.LocalDateTime;

public record TicketHistoryRes(
        String id,
        String ticketId,
        TicketStatus previousStatus,
        TicketStatus newStatus,
        String changedBy,
        String reason,
        LocalDateTime changedAt
) {
}


