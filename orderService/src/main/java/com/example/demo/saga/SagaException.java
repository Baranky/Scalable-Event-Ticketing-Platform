package com.example.demo.saga;

public class SagaException extends RuntimeException {

    private final SagaStep failedStep;

    public SagaException(String message, SagaStep failedStep) {
        super(message);
        this.failedStep = failedStep;
    }

    public SagaException(String message, SagaStep failedStep, Throwable cause) {
        super(message, cause);
        this.failedStep = failedStep;
    }

    public SagaStep getFailedStep() {
        return failedStep;
    }
}

