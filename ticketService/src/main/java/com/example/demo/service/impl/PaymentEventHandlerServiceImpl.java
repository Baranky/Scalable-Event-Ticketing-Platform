package com.example.demo.service.impl;

import com.example.demo.model.TicketStock;
import com.example.demo.repository.TicketStockRepository;
import com.example.demo.service.PaymentEventHandlerService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
public class PaymentEventHandlerServiceImpl implements PaymentEventHandlerService {

    private final TicketStockRepository ticketStockRepository;

    public PaymentEventHandlerServiceImpl(TicketStockRepository ticketStockRepository) {
        this.ticketStockRepository = ticketStockRepository;
    }

    @Override
    @Transactional
    public void handlePaymentSuccess(String orderId) {

        System.out.println("Payment success event received for order: " + orderId);
        System.out.println("Tickets already created via purchaseTickets API");
    }

    @Override
    @Transactional
    public void handlePaymentFailed(String orderId) {

        System.out.println("Payment failed event received for order: " + orderId);
        System.out.println("Stock unlock should be handled by OrderService");

    }

    public void handleOrderCancelled(String eventId, String priceCategoryId, int count) {
        try {
            TicketStock stock = ticketStockRepository
                    .findByEventIdAndPriceCategoryIdWithLock(eventId, priceCategoryId)
                    .orElse(null);

            if (stock != null && stock.getLockedCount() >= count) {
                stock.unlockTickets(count);
                ticketStockRepository.save(stock);
                System.out.println("Unlocked " + count + " tickets for event: " + eventId);
            }
        } catch (Exception e) {
            System.err.println("Failed to unlock tickets: " + e.getMessage());
        }
    }
}
