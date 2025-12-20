package com.example.demo.service.impl;

import com.example.demo.client.OrderClient;
import com.example.demo.service.TicketPdfService;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class TicketPdfServiceImpl implements TicketPdfService {

    @Override
    public File generateTicketPdf(OrderClient.OrderResponse order) {
        try {
            File pdfFile = File.createTempFile("ticket_" + order.id() + "_", ".pdf");

            java.io.FileWriter writer = new java.io.FileWriter(pdfFile);
            writer.write("BİLETİX - BİLET\n");
            writer.write("================\n\n");
            writer.write("Sipariş Bilgileri:\n");
            writer.write("Sipariş No: " + order.id() + "\n");
            writer.write("Durum: " + order.status() + "\n");
            writer.write("Toplam Tutar: " + order.totalAmount() + " " + order.currency() + "\n");
            writer.write("Bilet Sayısı: " + order.items().size() + "\n\n");
            writer.write("Bilet Detayları:\n");
            writer.write("-----------------\n");
            for (OrderClient.OrderItemDto item : order.items()) {
                writer.write("Bilet ID: " + item.ticketId() + "\n");
                writer.write("Etkinlik ID: " + item.eventId() + "\n");
                writer.write("Fiyat: " + item.price() + " TL\n");
                writer.write("---\n");
            }
            writer.write("\nOluşturulma Tarihi: "
                    + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")) + "\n");
            writer.close();

            return pdfFile;

        } catch (Exception e) {
            System.err.println("Failed to generate ticket PDF: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}
