package com.example.demo.service.impl;

import com.example.demo.enums.TicketStatus;
import com.example.demo.dto.TicketHistoryRes;
import com.example.demo.model.Ticket;
import com.example.demo.model.TicketHistory;
import com.example.demo.repository.TicketHistoryRepository;
import com.example.demo.service.TicketHistoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TicketHistoryServiceImpl implements TicketHistoryService {

    private final TicketHistoryRepository ticketHistoryRepository;

    public TicketHistoryServiceImpl(TicketHistoryRepository ticketHistoryRepository) {
        this.ticketHistoryRepository = ticketHistoryRepository;
    }

    @Override
    @Transactional
    public void recordStatusChange(Ticket ticket,
                                   TicketStatus previousStatus,
                                   TicketStatus newStatus,
                                   String changedBy,
                                   String reason) {
        TicketHistory history = new TicketHistory();
        history.setTicketId(ticket.getId());
        history.setPreviousStatus(previousStatus);
        history.setNewStatus(newStatus);
        history.setChangedBy(changedBy);
        history.setReason(reason);
        ticketHistoryRepository.save(history);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketHistoryRes> getHistoryByTicketId(String ticketId) {
        return ticketHistoryRepository.findByTicketIdOrderByChangedAtAsc(ticketId)
                .stream()
                .map(this::mapToTicketHistoryRes)
                .collect(Collectors.toList());
    }

    private TicketHistoryRes mapToTicketHistoryRes(TicketHistory history) {
        return new TicketHistoryRes(
                history.getId(),
                history.getTicketId(),
                history.getPreviousStatus(),
                history.getNewStatus(),
                history.getChangedBy(),
                history.getReason(),
                history.getChangedAt()
        );
    }
}


