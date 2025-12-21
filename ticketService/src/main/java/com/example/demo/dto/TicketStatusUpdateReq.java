package com.example.demo.dto;

import com.example.demo.enums.TicketStatus;

public record TicketStatusUpdateReq(
        TicketStatus newStatus,
        String changedBy,
        String reason
) {
}


