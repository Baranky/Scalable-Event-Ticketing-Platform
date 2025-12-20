package com.example.demo.service.impl;

import com.example.demo.client.OrderClient;
import com.example.demo.service.OrderCompletedHandlerService;
import com.example.demo.service.TicketPdfService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.mail.internet.MimeMessage;
import java.io.File;

@Service
public class OrderCompletedHandlerServiceImpl implements OrderCompletedHandlerService {

    private final OrderClient orderClient;
    private final TicketPdfService ticketPdfService;
    private final JavaMailSender mailSender;

    public OrderCompletedHandlerServiceImpl(OrderClient orderClient,
            TicketPdfService ticketPdfService,
            @Autowired(required = false) JavaMailSender mailSender) {
        this.orderClient = orderClient;
        this.ticketPdfService = ticketPdfService;
        this.mailSender = mailSender;
    }

    @Override
    @Transactional
    public void handleOrderCompleted(String orderId, String userId, String paymentId) {
        try {
            OrderClient.OrderResponse order = orderClient.getOrderById(orderId);

            if (order == null || order.items() == null || order.items().isEmpty()) {
                System.err.println("Order not found or has no items: " + orderId);
                return;
            }

            File pdfFile = ticketPdfService.generateTicketPdf(order);

            if (mailSender != null) {
                sendTicketEmail(userId, orderId, pdfFile);
            } else {
                System.out.println("Mail sender not configured, skipping email for order: " + orderId);
            }

            if (pdfFile != null && pdfFile.exists()) {
                pdfFile.delete();
            }

        } catch (Exception e) {
            System.err.println("Failed to handle order completed for order: " + orderId + ", error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void sendTicketEmail(String userId, String orderId, File pdfFile) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            // TODO: User Service'ten email adresini al
            String userEmail = userId + "@example.com"; // Placeholder

            helper.setTo(userEmail);
            helper.setSubject("Biletiniz Hazır - Sipariş #" + orderId);
            helper.setText("Merhaba,\n\n"
                    + "Siparişiniz başarıyla tamamlandı. Biletiniz ekte gönderilmiştir.\n\n"
                    + "İyi eğlenceler!\n\n"
                    + "Biletix Ekibi");

            if (pdfFile != null && pdfFile.exists()) {
                helper.addAttachment("bilet_" + orderId + ".pdf", pdfFile);
            }

            mailSender.send(message);
            System.out.println("Ticket email sent successfully to: " + userEmail);

        } catch (Exception e) {
            System.err.println("Failed to send ticket email: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
