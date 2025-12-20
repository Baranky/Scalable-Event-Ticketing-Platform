package com.example.demo.service;

import com.example.demo.client.OrderClient;
import java.io.File;

public interface TicketPdfService {

    File generateTicketPdf(OrderClient.OrderResponse order);
}
