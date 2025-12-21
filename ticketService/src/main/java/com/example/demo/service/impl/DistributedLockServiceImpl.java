package com.example.demo.service.impl;

import com.example.demo.exception.LockAcquisitionException;
import com.example.demo.service.DistributedLockService;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

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
            // Kilit zaten serbest bırakılmış veya expire olmuş
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


    public <T> T executeWithMultiLock(String[] lockKeys, long waitTime, long leaseTime, TimeUnit timeUnit, Supplier<T> action) {
        RLock[] locks = new RLock[lockKeys.length];
        for (int i = 0; i < lockKeys.length; i++) {
            locks[i] = redissonClient.getLock(lockKeys[i]);
        }
        
        RLock multiLock = redissonClient.getMultiLock(locks);
        boolean acquired = false;
        
        try {
            acquired = multiLock.tryLock(waitTime, leaseTime, timeUnit);
            
            if (!acquired) {
                throw new LockAcquisitionException(String.join(",", lockKeys), 
                        "Could not acquire multi-lock");
            }
            
            System.out.println("[MultiLock] All locks acquired: " + String.join(", ", lockKeys));
            
            return action.get();
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LockAcquisitionException(String.join(",", lockKeys), e);
        } finally {
            if (acquired) {
                multiLock.unlock();
                System.out.println(" [MultiLock] All locks released");
            }
        }
    }

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
}

