package com.example.demo.service;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;


public interface DistributedLockService {


    boolean tryLock(String lockKey, long waitTime, long leaseTime, TimeUnit timeUnit);


    void unlock(String lockKey);


    boolean isLocked(String lockKey);


    <T> T executeWithLock(String lockKey, long waitTime, long leaseTime, TimeUnit timeUnit, Supplier<T> action);


    <T> T executeWithLock(String lockKey, Supplier<T> action);


    <T> T executeWithFairLock(String lockKey, long waitTime, long leaseTime, TimeUnit timeUnit, Supplier<T> action);

    static String createSeatLockKey(String eventId, String seatLabel) {
        return "lock:event:" + eventId + ":seat:" + seatLabel;
    }


    static String createStockLockKey(String stockId) {
        return "lock:stock:" + stockId;
    }


    static String createOrderLockKey(String orderId) {
        return "lock:order:" + orderId;
    }
}

