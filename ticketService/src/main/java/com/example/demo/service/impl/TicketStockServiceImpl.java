package com.example.demo.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dto.PriceCategoryDetail;
import com.example.demo.dto.TicketStockRes;
import com.example.demo.model.TicketStock;
import com.example.demo.repository.TicketStockRepository;
import com.example.demo.service.SeatLockService;
import com.example.demo.service.TicketStockService;

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
        }
    }

    @Override
    public Optional<TicketStockRes> getStockById(String stockId) {
        return ticketStockRepository.findById(stockId)
                .map(this::mapToResponse);
    }

    @Override
    public List<TicketStockRes> getStocksByEventId(String eventId) {
        return ticketStockRepository.findByEventId(eventId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<TicketStockRes> getStockByEventAndPriceCategory(String eventId, String priceCategoryId) {
        return ticketStockRepository.findByEventIdAndPriceCategoryId(eventId, priceCategoryId)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional
    public boolean lockTickets(String stockId, int count, String orderId, List<String> seatLabels) {
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
    @Transactional(readOnly = true)
    public Optional<TicketStock> getStockEntityByIdWithLock(String stockId) {
        return ticketStockRepository.findByIdWithLock(stockId);
    }

    @Override
    @Transactional
    public void decrementAvailableAndIncrementSold(String stockId, int quantity) {
        Optional<TicketStock> stockOpt = ticketStockRepository.findByIdWithLock(stockId);
        if (stockOpt.isEmpty()) {
            throw new RuntimeException("Stock not found: " + stockId);
        }
        TicketStock stock = stockOpt.get();
        if (stock.getAvailableCount() < quantity) {
            throw new RuntimeException("Not enough tickets available. Requested: " + quantity
                    + ", Available: " + stock.getAvailableCount());
        }
        stock.setAvailableCount(stock.getAvailableCount() - quantity);
        stock.setSoldCount(stock.getSoldCount() + quantity);
        ticketStockRepository.save(stock);
    }

    @Override
    @Transactional
    public void incrementAvailableAndDecrementSold(String eventId, String priceCategoryId) {
        Optional<TicketStock> stockOpt = ticketStockRepository.findByEventIdAndPriceCategoryIdWithLock(
                eventId, priceCategoryId);
        if (stockOpt.isPresent()) {
            TicketStock stock = stockOpt.get();
            stock.setAvailableCount(stock.getAvailableCount() + 1);
            stock.setSoldCount(stock.getSoldCount() - 1);
            ticketStockRepository.save(stock);
        }
    }

    private TicketStockRes mapToResponse(TicketStock stock) {
        return new TicketStockRes(
                stock.getId(),
                stock.getEventId(),
                stock.getVenueId(),
                stock.getSectionId(),
                stock.getPriceCategoryId(),
                stock.getPrice(),
                stock.getCurrency(),
                stock.getTotalCount(),
                stock.getAvailableCount(),
                stock.getSoldCount(),
                stock.getLockedCount(),
                stock.getCreatedAt(),
                stock.getUpdatedAt()
        );
    }
}
