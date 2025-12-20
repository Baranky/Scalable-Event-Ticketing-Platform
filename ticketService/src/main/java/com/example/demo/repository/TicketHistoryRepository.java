package com.example.demo.repository;

import com.example.demo.model.TicketHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketHistoryRepository extends JpaRepository<TicketHistory, String> {

    List<TicketHistory> findByTicketIdOrderByChangedAtAsc(String ticketId);

}


