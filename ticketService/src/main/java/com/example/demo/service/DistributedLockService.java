package com.example.demo.service;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public interface DistributedLockService {

    boolean tryLock(String lockKey, long waitTime, long leaseTime, TimeUnit timeUnit);

    void unlock(String lockKey);

    boolean isLocked(String lockKey);

    <T> T executeWithLock(String lockKey, long waitTime, long leaseTime, TimeUnit timeUnit, Supplier<T> action);

    <T> T executeWithLock(String lockKey, Supplier<T> action);

    <T> T executeWithFairLock(String lockKey, long waitTime, long leaseTime, TimeUnit timeUnit, Supplier<T> action);

    String getLockInfo(String lockKey);

    Map<String, Object> lockSeat(String eventId, String seatLabel, String orderId);

    Map<String, Object> unlockSeat(String eventId, String seatLabel);

    Map<String, Object> checkLockStatus(String eventId, String seatLabel);

    Map<String, Object> testRaceCondition(String eventId, String seatLabel, int threads);

    Map<String, Object> testFairLock(String eventId, String seatLabel, int threads);

    static String createSeatLockKey(String eventId, String seatLabel) {
        return "lock:event:" + eventId + ":seat:" + seatLabel;
    }

}
