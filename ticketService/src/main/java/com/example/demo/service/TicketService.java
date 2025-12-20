package com.example.demo.service;

import com.example.demo.dto.TicketHistoryRes;
import com.example.demo.dto.TicketPurchaseReq;
import com.example.demo.dto.TicketRes;

import java.util.List;

public interface TicketService {


    List<TicketRes> purchaseTickets(TicketPurchaseReq request);

    TicketRes getTicketById(String id);


    List<TicketRes> getTicketsByEventId(String eventId);


    List<TicketRes> getTicketsByUserId(String userId);


    TicketRes useTicket(String ticketId, String usedBy);


    TicketRes refundTicket(String ticketId, String refundedBy, String reason);

    List<TicketHistoryRes> getTicketHistory(String ticketId);

}
