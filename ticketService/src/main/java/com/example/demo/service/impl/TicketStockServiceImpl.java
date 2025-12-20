package com.example.demo.service.impl;

import com.example.demo.consumer.EventCreatedConsumer.PriceCategoryDetail;
import com.example.demo.model.TicketStock;
import com.example.demo.repository.TicketStockRepository;
import com.example.demo.service.SeatLockService;
import com.example.demo.service.TicketStockService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class TicketStockServiceImpl implements TicketStockService {

    private final TicketStockRepository ticketStockRepository;
    private final SeatLockService seatLockService;

    public TicketStockServiceImpl(TicketStockRepository ticketStockRepository,
            SeatLockService seatLockService) {
        this.ticketStockRepository = ticketStockRepository;
        this.seatLockService = seatLockService;
    }

    @Override
    @Transactional
    public void createTicketStockForEvent(String eventId, String venueId, List<PriceCategoryDetail> priceCategories) {
        if (priceCategories == null || priceCategories.isEmpty()) {
            System.out.println("No price categories found for event: " + eventId + ", skipping stock creation");
            return;
        }

        List<TicketStock> stocksToCreate = new ArrayList<>();

        for (PriceCategoryDetail pc : priceCategories) {
            if (ticketStockRepository.existsByEventIdAndPriceCategoryId(eventId, pc.priceCategoryId())) {
                System.out.println("Stock already exists for event: " + eventId
                        + ", priceCategory: " + pc.priceCategoryId() + ", skipping...");
                continue;
            }

            TicketStock stock = new TicketStock();
            stock.setEventId(eventId);
            stock.setVenueId(venueId);
            stock.setSectionId(pc.sectionId());
            stock.setPriceCategoryId(pc.priceCategoryId());
            stock.setPrice(pc.price());
            stock.setCurrency(pc.currency() != null ? pc.currency() : "TRY");
            stock.setTotalCount(pc.totalAllocation());
            stock.setAvailableCount(pc.totalAllocation());
            stock.setSoldCount(0);
            stock.setLockedCount(0);

            stocksToCreate.add(stock);
        }

        if (!stocksToCreate.isEmpty()) {
            ticketStockRepository.saveAll(stocksToCreate);
            int totalCapacity = stocksToCreate.stream().mapToInt(TicketStock::getTotalCount).sum();
            System.out.println("Created " + stocksToCreate.size() + " stock records for event: " + eventId
                    + " (Total capacity: " + totalCapacity + " tickets)");
        }
    }

    @Override
    public Optional<TicketStock> getStockById(String stockId) {
        return ticketStockRepository.findById(stockId);
    }

    @Override
    public List<TicketStock> getStocksByEventId(String eventId) {
        return ticketStockRepository.findByEventId(eventId);
    }

    @Override
    public Optional<TicketStock> getStockByEventAndPriceCategory(String eventId, String priceCategoryId) {
        return ticketStockRepository.findByEventIdAndPriceCategoryId(eventId, priceCategoryId);
    }

    @Override
    @Transactional
    public boolean lockTickets(String stockId, int count, String orderId, List<String> seatLabels) {
        // 1. DB'den stok bilgisini al (pessimistic lock ile)
        Optional<TicketStock> stockOpt = ticketStockRepository.findByIdWithLock(stockId);
        if (stockOpt.isEmpty()) {
            System.err.println("Stock not found: " + stockId);
            return false;
        }

        TicketStock stock = stockOpt.get();

        if (stock.getAvailableCount() < count) {
            System.err.println("Not enough available tickets. Requested: " + count
                    + ", Available: " + stock.getAvailableCount());
            return false;
        }

        boolean redisLocked;
        if (seatLabels != null && !seatLabels.isEmpty()) {
            List<String> lockedSeats = seatLockService.lockSeats(stockId, seatLabels, orderId);
            redisLocked = !lockedSeats.isEmpty();
        } else {
            redisLocked = seatLockService.lockGenericSeats(stockId, count, orderId, stock.getTotalCount());
        }

        if (!redisLocked) {
            System.err.println("Failed to lock seats in Redis for stock: " + stockId + ", order: " + orderId);
            return false;
        }

        stock.setAvailableCount(stock.getAvailableCount() - count);
        stock.setLockedCount(stock.getLockedCount() + count);
        ticketStockRepository.save(stock);

        System.out.println("Locked " + count + " tickets for stock: " + stockId + ", order: " + orderId
                + " [Redis: ✓, DB: ✓]");
        return true;
    }

    @Override
    @Transactional
    public boolean confirmSale(String stockId, int count, String orderId) {
        Optional<TicketStock> stockOpt = ticketStockRepository.findByIdWithLock(stockId);
        if (stockOpt.isEmpty()) {
            System.err.println("Stock not found: " + stockId);
            return false;
        }

        TicketStock stock = stockOpt.get();

        if (stock.getLockedCount() < count) {
            System.err.println("Not enough locked tickets. Requested: " + count
                    + ", Locked: " + stock.getLockedCount());
            return false;
        }

        seatLockService.confirmGenericSale(stockId, count, orderId);

        stock.setLockedCount(stock.getLockedCount() - count);
        stock.setSoldCount(stock.getSoldCount() + count);
        ticketStockRepository.save(stock);

        System.out.println("Confirmed sale of " + count + " tickets for stock: " + stockId + ", order: " + orderId
                + " [Redis: cleared, DB: sold]");
        return true;
    }

    @Override
    @Transactional
    public boolean unlockTickets(String stockId, int count, String orderId) {
        Optional<TicketStock> stockOpt = ticketStockRepository.findByIdWithLock(stockId);
        if (stockOpt.isEmpty()) {
            System.err.println("Stock not found: " + stockId);
            return false;
        }

        TicketStock stock = stockOpt.get();

        seatLockService.unlockGenericSeats(stockId, count, orderId);

        int actualUnlock = Math.min(count, stock.getLockedCount());
        stock.setLockedCount(stock.getLockedCount() - actualUnlock);
        stock.setAvailableCount(stock.getAvailableCount() + actualUnlock);
        ticketStockRepository.save(stock);

        System.out.println("Unlocked " + actualUnlock + " tickets for stock: " + stockId + ", order: " + orderId
                + " [Redis: cleared, DB: available]");
        return true;
    }

    @Override
    public int getRedisLockedCount(String stockId) {
        return seatLockService.getLockedCount(stockId);
    }

    @Override
    @Transactional
    public TicketStock save(TicketStock stock) {
        return ticketStockRepository.save(stock);
    }
}
