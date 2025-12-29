package com.example.demo.service.impl;

import com.example.demo.exception.LockAcquisitionException;
import com.example.demo.service.DistributedLockService;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Service
public class DistributedLockServiceImpl implements DistributedLockService {

    private static final long DEFAULT_WAIT_TIME = 10;
    private static final long DEFAULT_LEASE_TIME = 30;

    private final RedissonClient redissonClient;

    public DistributedLockServiceImpl(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    public boolean tryLock(String lockKey, long waitTime, long leaseTime, TimeUnit timeUnit) {
        RLock lock = redissonClient.getLock(lockKey);
        try {
            boolean acquired = lock.tryLock(waitTime, leaseTime, timeUnit);
            if (acquired) {
                System.out.println("Lock acquired: " + lockKey + " by thread: " + Thread.currentThread().getName());
            } else {
                System.out.println("Lock not acquired (timeout): " + lockKey);
            }
            return acquired;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Lock acquisition interrupted: " + lockKey);
            return false;
        }
    }

    @Override
    public void unlock(String lockKey) {
        RLock lock = redissonClient.getLock(lockKey);
        try {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                System.out.println("Lock released: " + lockKey + " by thread: " + Thread.currentThread().getName());
            } else {
                System.out.println(" Lock not held by current thread, cannot unlock: " + lockKey);
            }
        } catch (IllegalMonitorStateException e) {
            System.out.println("Lock already released or expired: " + lockKey);
        }
    }

    @Override
    public boolean isLocked(String lockKey) {
        RLock lock = redissonClient.getLock(lockKey);
        return lock.isLocked();
    }

    @Override
    public <T> T executeWithLock(String lockKey, long waitTime, long leaseTime, TimeUnit timeUnit, Supplier<T> action) {
        RLock lock = redissonClient.getLock(lockKey);
        boolean acquired = false;
        
        try {
            acquired = lock.tryLock(waitTime, leaseTime, timeUnit);
            
            if (!acquired) {
                throw new LockAcquisitionException(lockKey, 
                        "Could not acquire lock within " + waitTime + " " + timeUnit);
            }
            
            System.out.println("[executeWithLock] Lock acquired: " + lockKey);
            
            return action.get();
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LockAcquisitionException(lockKey, e);
        } finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
                System.out.println(" [executeWithLock] Lock released: " + lockKey);
            }
        }
    }

    @Override
    public <T> T executeWithLock(String lockKey, Supplier<T> action) {
        return executeWithLock(lockKey, DEFAULT_WAIT_TIME, DEFAULT_LEASE_TIME, TimeUnit.SECONDS, action);
    }

    @Override
    public <T> T executeWithFairLock(String lockKey, long waitTime, long leaseTime, TimeUnit timeUnit, Supplier<T> action) {
        RLock fairLock = redissonClient.getFairLock(lockKey);
        boolean acquired = false;

        try {
            acquired = fairLock.tryLock(waitTime, leaseTime, timeUnit);

            if (!acquired) {
                throw new LockAcquisitionException(lockKey,
                        "Could not acquire fair lock within " + waitTime + " " + timeUnit);
            }

            System.out.println(" [FairLock] Lock acquired: " + lockKey + " (FIFO order)");

            return action.get();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LockAcquisitionException(lockKey, e);
        } finally {
            if (acquired && fairLock.isHeldByCurrentThread()) {
                fairLock.unlock();
                System.out.println(" [FairLock] Lock released: " + lockKey);
            }
        }
    }

    @Override
    public String getLockInfo(String lockKey) {
        RLock lock = redissonClient.getLock(lockKey);
        return String.format(
                "Lock[key=%s, isLocked=%s, holdCount=%d, remainTTL=%dms]",
                lockKey,
                lock.isLocked(),
                lock.getHoldCount(),
                lock.remainTimeToLive()
        );
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
            boolean acquired = tryLock(lockKey, 5, 30, TimeUnit.SECONDS);
            response.put("acquired", acquired);
            response.put("message", acquired ? "Kilit alındı!" : "Kilit alınamadı (başka biri tutuyor)");

            if (acquired) {
                response.put("lockInfo", getLockInfo(lockKey));
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
            unlock(lockKey);
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
        response.put("isLocked", isLocked(lockKey));
        response.put("lockInfo", getLockInfo(lockKey));

        return response;
    }

    @Override
    public Map<String, Object> testRaceCondition(String eventId, String seatLabel, int threads) {
        String lockKey = DistributedLockService.createSeatLockKey(eventId, seatLabel);

        Map<String, Object> response = new HashMap<>();
        response.put("lockKey", lockKey);
        response.put("threadCount", threads);

        try {
            unlock(lockKey);
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
                    boolean acquired = tryLock(lockKey, 1, 30, TimeUnit.SECONDS);
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
        response.put("isLocked", isLocked(lockKey));

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
                    String result = executeWithFairLock(
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

