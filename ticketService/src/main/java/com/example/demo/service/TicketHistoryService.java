package com.example.demo.service;

import com.example.demo.enums.TicketStatus;
import com.example.demo.dto.TicketHistoryRes;
import com.example.demo.entity.Ticket;

import java.util.List;

public interface TicketHistoryService {

    void recordStatusChange(Ticket ticket,
                            TicketStatus previousStatus,
                            TicketStatus newStatus,
                            String changedBy,
                            String reason);

    List<TicketHistoryRes> getHistoryByTicketId(String ticketId);
}


