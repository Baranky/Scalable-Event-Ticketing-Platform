package com.example.demo.controller;

import com.example.demo.service.DistributedLockService;
import com.example.demo.service.SeatLockService;
import com.example.demo.service.impl.DistributedLockServiceImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;


@RestController
@RequestMapping("/api/locks")
public class DistributedLockController {

    private final DistributedLockService distributedLockService;
    private final SeatLockService seatLockService;
    private final DistributedLockServiceImpl lockServiceImpl;

    public DistributedLockController(
            DistributedLockService distributedLockService,
            SeatLockService seatLockService,
            DistributedLockServiceImpl lockServiceImpl) {
        this.distributedLockService = distributedLockService;
        this.seatLockService = seatLockService;
        this.lockServiceImpl = lockServiceImpl;
    }


    @PostMapping("/seat")
    public ResponseEntity<Map<String, Object>> lockSeat(
            @RequestParam String eventId,
            @RequestParam String seatLabel,
            @RequestParam String orderId) {

        String lockKey = DistributedLockService.createSeatLockKey(eventId, seatLabel);

        Map<String, Object> response = new HashMap<>();
        response.put("lockKey", lockKey);
        response.put("eventId", eventId);
        response.put("seatLabel", seatLabel);
        response.put("orderId", orderId);

        try {
            boolean acquired = distributedLockService.tryLock(lockKey, 5, 30, TimeUnit.SECONDS);
            response.put("acquired", acquired);
            response.put("message", acquired ? "Kilit alındı!" : "Kilit alınamadı (başka biri tutuyor)");

            if (acquired) {
                response.put("lockInfo", lockServiceImpl.getLockInfo(lockKey));
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("acquired", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    @DeleteMapping("/seat")
    public ResponseEntity<Map<String, Object>> unlockSeat(
            @RequestParam String eventId,
            @RequestParam String seatLabel) {
        
        String lockKey = DistributedLockService.createSeatLockKey(eventId, seatLabel);
        
        Map<String, Object> response = new HashMap<>();
        response.put("lockKey", lockKey);
        
        try {
            distributedLockService.unlock(lockKey);
            response.put("released", true);
            response.put("message", "Kilit serbest bırakıldı");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("released", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping("/seat/status")
    public ResponseEntity<Map<String, Object>> checkLockStatus(
            @RequestParam String eventId,
            @RequestParam String seatLabel) {
        
        String lockKey = DistributedLockService.createSeatLockKey(eventId, seatLabel);
        
        Map<String, Object> response = new HashMap<>();
        response.put("lockKey", lockKey);
        response.put("isLocked", distributedLockService.isLocked(lockKey));
        response.put("lockInfo", lockServiceImpl.getLockInfo(lockKey));
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/stock/{stockId}/seats")
    public ResponseEntity<Map<String, Object>> getLockedSeats(@PathVariable String stockId) {
        Map<String, Object> response = new HashMap<>();
        response.put("stockId", stockId);
        response.put("lockedSeats", seatLockService.getLockedSeats(stockId));
        response.put("totalLockedCount", seatLockService.getLockedCount(stockId));
        
        return ResponseEntity.ok(response);
    }


    @PostMapping("/test-race")
    public ResponseEntity<Map<String, Object>> testRaceCondition(
            @RequestParam String eventId,
            @RequestParam String seatLabel,
            @RequestParam(defaultValue = "100") int threads) {
        
        String lockKey = DistributedLockService.createSeatLockKey(eventId, seatLabel);
        
        Map<String, Object> response = new HashMap<>();
        response.put("lockKey", lockKey);
        response.put("threadCount", threads);
        
        try {
            distributedLockService.unlock(lockKey);
        } catch (Exception ignored) {}
        
        final int[] successCount = {0};
        final int[] failCount = {0};
        final String[] winner = {null};
        
        Thread[] threadArray = new Thread[threads];
        for (int i = 0; i < threads; i++) {
            final int threadId = i;
            threadArray[i] = new Thread(() -> {
                try {
                    boolean acquired = distributedLockService.tryLock(lockKey, 1, 30, TimeUnit.SECONDS);
                    if (acquired) {
                        synchronized (successCount) {
                            successCount[0]++;
                            if (winner[0] == null) {
                                winner[0] = "Thread-" + threadId;
                            }
                        }
                        // Kilidi tut (test için)
                    } else {
                        synchronized (failCount) {
                            failCount[0]++;
                        }
                    }
                } catch (Exception e) {
                    synchronized (failCount) {
                        failCount[0]++;
                    }
                }
            });
        }
        
        long startTime = System.currentTimeMillis();
        for (Thread t : threadArray) {
            t.start();
        }
        
        for (Thread t : threadArray) {
            try {
                t.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        long duration = System.currentTimeMillis() - startTime;
        
        response.put("successCount", successCount[0]);
        response.put("failCount", failCount[0]);
        response.put("winner", winner[0]);
        response.put("durationMs", duration);
        response.put("isLocked", distributedLockService.isLocked(lockKey));
        
        // Test sonucu
        boolean testPassed = successCount[0] == 1;
        response.put("testPassed", testPassed);
        response.put("message", testPassed 
                ? "BAŞARILI: Sadece 1 thread kilidi aldı (Distributed Lock çalışıyor!)"
                : " BAŞARISIZ: " + successCount[0] + " thread kilidi aldı (Race condition!)");
        
        return ResponseEntity.ok(response);
    }


    @PostMapping("/test-fair")
    public ResponseEntity<Map<String, Object>> testFairLock(
            @RequestParam String eventId,
            @RequestParam String seatLabel,
            @RequestParam(defaultValue = "10") int threads) {
        
        String lockKey = "fair:" + DistributedLockService.createSeatLockKey(eventId, seatLabel);
        
        Map<String, Object> response = new HashMap<>();
        response.put("lockKey", lockKey);
        response.put("threadCount", threads);
        
        final List<String> completionOrder = new java.util.concurrent.CopyOnWriteArrayList<>();
        
        Thread[] threadArray = new Thread[threads];
        for (int i = 0; i < threads; i++) {
            final int threadId = i;
            threadArray[i] = new Thread(() -> {
                try {
                    String result = distributedLockService.executeWithFairLock(
                            lockKey, 30, 5, TimeUnit.SECONDS,
                            () -> {
                                completionOrder.add("Thread-" + threadId);
                                try {
                                    Thread.sleep(100); // Simüle işlem
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                }
                                return "Thread-" + threadId + " completed";
                            }
                    );
                } catch (Exception e) {
                    completionOrder.add("Thread-" + threadId + " (FAILED)");
                }
            });
        }
        
        long startTime = System.currentTimeMillis();
        for (Thread t : threadArray) {
            t.start();
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        for (Thread t : threadArray) {
            try {
                t.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        long duration = System.currentTimeMillis() - startTime;
        
        response.put("completionOrder", completionOrder);
        response.put("durationMs", duration);
        response.put("message", "Fair Lock ile FIFO sırasında işlem yapıldı");
        
        return ResponseEntity.ok(response);
    }
}

