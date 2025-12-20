package com.example.demo.dto;

import com.example.demo.Enums.TicketStatus;

public record TicketStatusUpdateReq(
        TicketStatus newStatus,
        String changedBy,
        String reason
) {
}


