package com.example.demo.saga;

public record SagaStepResult(
        SagaStep step,
        boolean success,
        String message,
        Object data
) {
    
    public static SagaStepResult success(SagaStep step, String message) {
        return new SagaStepResult(step, true, message, null);
    }
    
    public static SagaStepResult success(SagaStep step, String message, Object data) {
        return new SagaStepResult(step, true, message, data);
    }
    
    public static SagaStepResult failure(SagaStep step, String message) {
        return new SagaStepResult(step, false, message, null);
    }
}

