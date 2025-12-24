package com.example.demo.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.example.demo.exception.LockAcquisitionException;
import com.example.demo.service.DistributedLockService;

@Service
public class DistributedLockServiceImpl implements DistributedLockService {

    private static final Logger log = LoggerFactory.getLogger(DistributedLockServiceImpl.class);

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
                log.info("Lock acquired: lockKey={}, thread={}", lockKey, Thread.currentThread().getName());
            } else {
                log.warn("Lock not acquired (timeout): lockKey={}", lockKey);
            }
            return acquired;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Lock acquisition interrupted: lockKey={}", lockKey, e);
            return false;
        }
    }

    @Override
    public void unlock(String lockKey) {
        RLock lock = redissonClient.getLock(lockKey);
        try {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.info("Lock released: lockKey={}, thread={}", lockKey, Thread.currentThread().getName());
            } else {
                log.warn("Lock not held by current thread, cannot unlock: lockKey={}", lockKey);
            }
        } catch (IllegalMonitorStateException e) {
            log.debug("Lock already released or expired: lockKey={}", lockKey);
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

            log.info("Lock acquired for execution: lockKey={}, thread={}", lockKey, Thread.currentThread().getName());

            return action.get();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LockAcquisitionException(lockKey, e);
        } finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.info("Lock released after execution: lockKey={}, thread={}", lockKey, Thread.currentThread().getName());
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

            log.info("Fair lock acquired (FIFO order): lockKey={}, thread={}", lockKey, Thread.currentThread().getName());

            return action.get();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LockAcquisitionException(lockKey, e);
        } finally {
            if (acquired && fairLock.isHeldByCurrentThread()) {
                fairLock.unlock();
                log.info("Fair lock released: lockKey={}, thread={}", lockKey, Thread.currentThread().getName());
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
        Map<String, Object> response = initializeRaceConditionResponse(lockKey, threads);

        ensureLockUnlocked(lockKey);

        final int[] successCount = {0};
        final int[] failCount = {0};
        final String[] winner = {null};

        Thread[] threadArray = createRaceConditionThreads(lockKey, threads, successCount, failCount, winner);
        long duration = runThreadsAndWait(threadArray);

        buildRaceConditionResponse(response, successCount[0], failCount[0], winner[0], duration, lockKey);

        return response;
    }

    private Map<String, Object> initializeRaceConditionResponse(String lockKey, int threads) {
        Map<String, Object> response = new HashMap<>();
        response.put("lockKey", lockKey);
        response.put("threadCount", threads);
        return response;
    }

    private void ensureLockUnlocked(String lockKey) {
        try {
            unlock(lockKey);
        } catch (Exception ignored) {
            log.debug("Lock was already unlocked or does not exist: lockKey={}", lockKey);
        }
    }

    private Thread[] createRaceConditionThreads(String lockKey, int threadCount,
            int[] successCount, int[] failCount, String[] winner) {
        Thread[] threadArray = new Thread[threadCount];
        for (int i = 0; i < threadCount; i++) {
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
        return threadArray;
    }

    private long runThreadsAndWait(Thread[] threads) {
        long startTime = System.currentTimeMillis();

        for (Thread t : threads) {
            t.start();
        }

        for (Thread t : threads) {
            try {
                t.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Thread join interrupted", e);
            }
        }

        return System.currentTimeMillis() - startTime;
    }

    private void buildRaceConditionResponse(Map<String, Object> response, int successCount,
            int failCount, String winner, long duration, String lockKey) {
        response.put("successCount", successCount);
        response.put("failCount", failCount);
        response.put("winner", winner);
        response.put("durationMs", duration);
        response.put("isLocked", isLocked(lockKey));

        boolean testPassed = successCount == 1;
        response.put("testPassed", testPassed);
        response.put("message", testPassed
                ? "BAŞARILI: Sadece 1 thread kilidi aldı (Distributed Lock çalışıyor!)"
                : "BAŞARISIZ: " + successCount + " thread kilidi aldı (Race condition!)");
    }

    @Override
    public Map<String, Object> testFairLock(String eventId, String seatLabel, int threads) {
        String lockKey = "fair:" + DistributedLockService.createSeatLockKey(eventId, seatLabel);
        Map<String, Object> response = initializeFairLockResponse(lockKey, threads);

        final List<String> completionOrder = new java.util.concurrent.CopyOnWriteArrayList<>();
        Thread[] threadArray = createFairLockThreads(lockKey, threads, completionOrder);
        long duration = runThreadsWithDelay(threadArray);

        buildFairLockResponse(response, completionOrder, duration);

        return response;
    }

    private Map<String, Object> initializeFairLockResponse(String lockKey, int threads) {
        Map<String, Object> response = new HashMap<>();
        response.put("lockKey", lockKey);
        response.put("threadCount", threads);
        return response;
    }

    private Thread[] createFairLockThreads(String lockKey, int threadCount, List<String> completionOrder) {
        Thread[] threadArray = new Thread[threadCount];
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            threadArray[i] = new Thread(() -> {
                try {
                    executeWithFairLock(
                            lockKey, 30, 5, TimeUnit.SECONDS,
                            () -> {
                                completionOrder.add("Thread-" + threadId);
                                sleepSafely(100);
                                return "Thread-" + threadId + " completed";
                            }
                    );
                } catch (Exception e) {
                    completionOrder.add("Thread-" + threadId + " (FAILED)");
                    log.warn("Fair lock test thread failed: threadId={}, error={}", threadId, e.getMessage());
                }
            });
        }
        return threadArray;
    }

    private long runThreadsWithDelay(Thread[] threads) {
        long startTime = System.currentTimeMillis();

        for (Thread t : threads) {
            t.start();
            sleepSafely(10);
        }

        for (Thread t : threads) {
            try {
                t.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Thread join interrupted", e);
            }
        }

        return System.currentTimeMillis() - startTime;
    }

    private void sleepSafely(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.debug("Thread sleep interrupted");
        }
    }

    private void buildFairLockResponse(Map<String, Object> response, List<String> completionOrder, long duration) {
        response.put("completionOrder", completionOrder);
        response.put("durationMs", duration);
        response.put("message", "Fair Lock ile FIFO sırasında işlem yapıldı");
    }
}
