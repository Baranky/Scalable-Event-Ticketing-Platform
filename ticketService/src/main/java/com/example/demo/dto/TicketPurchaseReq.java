package com.example.demo.dto;

import java.util.List;


public record TicketPurchaseReq(
    String userId,              //
    String stockId,
    int quantity,
    List<String> seatLabels
) {
}

