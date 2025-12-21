package com.example.demo.service.impl;

import com.example.demo.service.DistributedLockService;
import com.example.demo.service.LockManagementService;
import com.example.demo.service.SeatLockService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class LockManagementServiceImpl implements LockManagementService {

    private final DistributedLockService distributedLockService;
    private final SeatLockService seatLockService;
    private final DistributedLockServiceImpl lockServiceImpl;

    public LockManagementServiceImpl(
            DistributedLockService distributedLockService,
            SeatLockService seatLockService,
            DistributedLockServiceImpl lockServiceImpl) {
        this.distributedLockService = distributedLockService;
        this.seatLockService = seatLockService;
        this.lockServiceImpl = lockServiceImpl;
    }

    @Override
    public Map<String, Object> lockSeat(String eventId, String seatLabel, String orderId) {
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

            return response;
        } catch (Exception e) {
            response.put("acquired", false);
            response.put("error", e.getMessage());
            return response;
        }
    }

    @Override
    public Map<String, Object> unlockSeat(String eventId, String seatLabel) {
        String lockKey = DistributedLockService.createSeatLockKey(eventId, seatLabel);

        Map<String, Object> response = new HashMap<>();
        response.put("lockKey", lockKey);

        try {
            distributedLockService.unlock(lockKey);
            response.put("released", true);
            response.put("message", "Kilit serbest bırakıldı");
            return response;
        } catch (Exception e) {
            response.put("released", false);
            response.put("error", e.getMessage());
            return response;
        }
    }

    @Override
    public Map<String, Object> checkLockStatus(String eventId, String seatLabel) {
        String lockKey = DistributedLockService.createSeatLockKey(eventId, seatLabel);

        Map<String, Object> response = new HashMap<>();
        response.put("lockKey", lockKey);
        response.put("isLocked", distributedLockService.isLocked(lockKey));
        response.put("lockInfo", lockServiceImpl.getLockInfo(lockKey));

        return response;
    }

    @Override
    public Map<String, Object> getLockedSeats(String stockId) {
        Map<String, Object> response = new HashMap<>();
        response.put("stockId", stockId);
        response.put("lockedSeats", seatLockService.getLockedSeats(stockId));
        response.put("totalLockedCount", seatLockService.getLockedCount(stockId));

        return response;
    }

    @Override
    public Map<String, Object> testRaceCondition(String eventId, String seatLabel, int threads) {
        String lockKey = DistributedLockService.createSeatLockKey(eventId, seatLabel);

        Map<String, Object> response = new HashMap<>();
        response.put("lockKey", lockKey);
        response.put("threadCount", threads);

        try {
            distributedLockService.unlock(lockKey);
        } catch (Exception ignored) {
        }

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

        boolean testPassed = successCount[0] == 1;
        response.put("testPassed", testPassed);
        response.put("message", testPassed
                ? "BAŞARILI: Sadece 1 thread kilidi aldı (Distributed Lock çalışıyor!)"
                : "BAŞARISIZ: " + successCount[0] + " thread kilidi aldı (Race condition!)");

        return response;
    }

    @Override
    public Map<String, Object> testFairLock(String eventId, String seatLabel, int threads) {
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
                                    Thread.sleep(100);
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

        return response;
    }
}

