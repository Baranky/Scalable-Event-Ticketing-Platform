package com.example.demo.scheduler;

import com.example.demo.entity.TicketStock;
import com.example.demo.repository.TicketStockRepository;
import com.example.demo.service.SeatLockService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class LockTimeoutScheduler {

    private final TicketStockRepository ticketStockRepository;
    private final SeatLockService seatLockService;

    public LockTimeoutScheduler(TicketStockRepository ticketStockRepository,
            SeatLockService seatLockService) {
        this.ticketStockRepository = ticketStockRepository;
        this.seatLockService = seatLockService;
    }


    @Scheduled(fixedRate = 60000)
    @Transactional
    public void syncLockedCountsWithRedis() {
        List<TicketStock> stocksWithLocks = ticketStockRepository.findByLockedCountGreaterThan(0);

        for (TicketStock stock : stocksWithLocks) {
            int redisLockedCount = seatLockService.getLockedCount(stock.getId());
            int dbLockedCount = stock.getLockedCount();

            if (redisLockedCount < dbLockedCount) {
                int expiredLocks = dbLockedCount - redisLockedCount;

                stock.setLockedCount(redisLockedCount);
                stock.setAvailableCount(stock.getAvailableCount() + expiredLocks);
                ticketStockRepository.save(stock);

                System.out.println("[LockTimeout] Stock " + stock.getId() + ": "
                        + expiredLocks + " expired locks returned to available. "
                        + "(DB locked: " + dbLockedCount + " -> " + redisLockedCount + ")");
            }
        }
    }
}
