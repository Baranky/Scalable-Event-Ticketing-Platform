package com.example.demo.scheduler;

import com.example.demo.entity.OrderOutbox;
import com.example.demo.repository.OrderOutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Component
public class OrderOutboxProcessor {

    private static final int BATCH_SIZE = 100;
    private static final long KAFKA_TIMEOUT_SECONDS = 10;
    private static final Logger log = LoggerFactory.getLogger(OrderOutboxProcessor.class);

    private final OrderOutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public OrderOutboxProcessor(OrderOutboxRepository outboxRepository,
                                KafkaTemplate<String, String> kafkaTemplate) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void processOutbox() {
        List<OrderOutbox> unprocessedEvents = outboxRepository.findUnprocessedWithLimit(BATCH_SIZE);

        if (unprocessedEvents.isEmpty()) {
            return;
        }

        log.info("ORDER OUTBOX PROCESSOR - {} event(s) will be processed", unprocessedEvents.size());

        int successCount = 0;
        int failCount = 0;

        for (OrderOutbox outbox : unprocessedEvents) {
            try {
                publishToKafka(outbox);
                outbox.markAsProcessed();
                outboxRepository.save(outbox);
                successCount++;
                log.info("Event sent successfully: type={}, aggregateId={}", outbox.getEventType(), outbox.getAggregateId());
            } catch (Exception e) {
                outbox.markAsFailed(e.getMessage());
                outboxRepository.save(outbox);
                failCount++;
                log.error("Event send failed: type={}, aggregateId={}, retryCount={}, error={}",
                        outbox.getEventType(), outbox.getAggregateId(), outbox.getRetryCount(), e.getMessage());
            }
        }

        log.info("Order outbox processing finished: success={}, failed={}", successCount, failCount);
    }


    private void publishToKafka(OrderOutbox outbox) throws Exception {
        CompletableFuture<?> future = kafkaTemplate.send(
                outbox.getTopic(),
                outbox.getAggregateId(),
                outbox.getPayload()
        );

        try {
            future.get(KAFKA_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new RuntimeException("Kafka send failed: " + e.getMessage(), e);
        }
    }


    @Scheduled(fixedDelay = 60000)
    public void reportDeadLetters() {
        long deadLetterCount = outboxRepository.countDeadLetters();
        if (deadLetterCount > 0) {
            log.warn("[ORDER] ALERT: {} dead letter event(s) detected", deadLetterCount);

            List<OrderOutbox> deadLetters = outboxRepository.findDeadLetterEvents();
            for (OrderOutbox dl : deadLetters) {
                log.warn("Dead Letter: type={}, aggregateId={}, lastError={}",
                        dl.getEventType(), dl.getAggregateId(), dl.getLastError());
            }
        }
    }

    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void cleanupProcessedEvents() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(7);
        int deletedCount = outboxRepository.deleteProcessedBefore(threshold);
        if (deletedCount > 0) {
            log.info("[ORDER] Cleanup: {} processed outbox record(s) deleted", deletedCount);
        }
    }


    public OutboxStats getStats() {
        return new OutboxStats(
                outboxRepository.countByProcessedFalse(),
                outboxRepository.countByProcessedTrue(),
                outboxRepository.countDeadLetters()
        );
    }

    public record OutboxStats(
            long pending,
            long processed,
            long deadLetters
    ) {}
}

