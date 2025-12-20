package com.example.demo.dto;

import com.example.demo.saga.SagaStatus;
import com.example.demo.saga.SagaStep;

import java.time.LocalDateTime;


public record SagaStatusResponse(
        String sagaId,
        String orderId,
        SagaStatus status,
        SagaStep currentStep,
        SagaStep failedStep,
        String errorMessage,
        String completedSteps,
        LocalDateTime createdAt,
        LocalDateTime completedAt
) {
}

