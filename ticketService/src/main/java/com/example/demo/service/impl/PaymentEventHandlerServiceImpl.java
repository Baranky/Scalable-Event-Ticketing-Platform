package com.example.demo.service.impl;

import com.example.demo.dto.PaymentEvent;
import com.example.demo.dto.TicketPurchaseReq;
import com.example.demo.service.PaymentEventHandlerService;
import com.example.demo.service.TicketService;
import com.example.demo.service.TicketStockService;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PaymentEventHandlerServiceImpl implements PaymentEventHandlerService {

    private final TicketStockService ticketStockService;
    private final TicketService ticketService;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    
    // ORDER_CREATED event'inden gelen seatLabels bilgisini saklamak için
    private final Map<String, List<String>> orderSeatLabelsMap = new ConcurrentHashMap<>();

    public PaymentEventHandlerServiceImpl(TicketStockService ticketStockService,
                                          TicketService ticketService,
                                          KafkaTemplate<String, String> kafkaTemplate,
                                          ObjectMapper objectMapper) {
        this.ticketStockService = ticketStockService;
        this.ticketService = ticketService;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    // ORDER_CREATED event'inden gelen seatLabels bilgisini saklamak için
    public void storeOrderSeatLabels(String orderId, List<String> seatLabels) {
        if (seatLabels != null && !seatLabels.isEmpty()) {
            orderSeatLabelsMap.put(orderId, seatLabels);
        }
    }

    @Override
    @Transactional
    public void handlePaymentSuccess(PaymentEvent event) {
        String orderId = event.orderId();
        String stockId = event.stockId();
        Integer quantity = event.quantity();
        String userId = event.userId();

        System.out.println("✅ Payment success event received for order: " + orderId);
        
        if (stockId == null || quantity == null) {
            System.err.println("❌ Missing stockId or quantity in PaymentEvent for orderId: " + orderId);
            return;
        }

        // 1. Biletlerin kilidini kaldır (confirmSale)
        System.out.println("🔓 Confirming sale and unlocking tickets for orderId: " + orderId);
        boolean confirmed = ticketStockService.confirmSale(stockId, quantity, orderId);
        
        if (!confirmed) {
            System.err.println("❌ Failed to confirm sale for orderId: " + orderId);
            return;
        }

        // 2. Biletleri oluştur (purchaseTickets)
        System.out.println("🎫 Creating tickets for orderId: " + orderId);
        List<String> seatLabels = orderSeatLabelsMap.remove(orderId); // Kullanıldıktan sonra temizle
        
        try {
            ticketService.purchaseTickets(new TicketPurchaseReq(
                    userId,
                    stockId,
                    quantity,
                    seatLabels
            ));
            System.out.println("✅ Tickets created successfully for orderId: " + orderId);
        } catch (Exception e) {
            System.err.println("❌ Failed to create tickets for orderId: " + orderId + ", error: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        // 3. Order Service'e TICKETS_SOLD event'i gönder (sipariş tamamlansın)
        System.out.println("📤 Sending TICKETS_SOLD event to Order Service for orderId: " + orderId);
        try {
            String ticketsSoldEvent = objectMapper.writeValueAsString(Map.of(
                    "eventType", "TICKETS_SOLD",
                    "orderId", orderId,
                    "userId", userId,
                    "stockId", stockId,
                    "quantity", quantity
            ));
            kafkaTemplate.send("ticket-events", orderId, ticketsSoldEvent);
            System.out.println("✅ TICKETS_SOLD event sent successfully");
        } catch (Exception e) {
            System.err.println("❌ Failed to send TICKETS_SOLD event: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void handlePaymentFailed(String orderId) {
        System.out.println("❌ Payment failed event received for order: " + orderId);
        System.out.println("Stock unlock should be handled by OrderService");
        orderSeatLabelsMap.remove(orderId); // Temizle
    }
}
