package com.example.demo.exception;

public class LockAcquisitionException extends RuntimeException {

    private final String lockKey;

    public LockAcquisitionException(String lockKey) {
        super("Failed to acquire lock: " + lockKey);
        this.lockKey = lockKey;
    }

    public LockAcquisitionException(String lockKey, String message) {
        super(message);
        this.lockKey = lockKey;
    }

    public LockAcquisitionException(String lockKey, Throwable cause) {
        super("Failed to acquire lock: " + lockKey, cause);
        this.lockKey = lockKey;
    }

    public String getLockKey() {
        return lockKey;
    }
}

