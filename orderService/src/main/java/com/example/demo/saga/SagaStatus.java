package com.example.demo.saga;


public enum SagaStatus {
    
    STARTED,
    IN_PROGRESS,
    COMPLETED,
    COMPENSATING,
    COMPENSATED,
    FAILED
}

