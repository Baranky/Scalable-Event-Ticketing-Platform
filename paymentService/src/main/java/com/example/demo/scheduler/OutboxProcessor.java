package com.example.demo.scheduler;

import com.example.demo.model.PaymentOutbox;
import com.example.demo.repository.PaymentOutboxRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;


@Component
public class OutboxProcessor {

    private static final int BATCH_SIZE = 100;
    private static final long KAFKA_TIMEOUT_SECONDS = 10;

    private final PaymentOutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public OutboxProcessor(PaymentOutboxRepository outboxRepository,
                          KafkaTemplate<String, String> kafkaTemplate) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void processOutbox() {
        List<PaymentOutbox> unprocessedEvents = outboxRepository.findUnprocessedWithLimit(BATCH_SIZE);

        if (unprocessedEvents.isEmpty()) {
            return;
        }


        int successCount = 0;
        int failCount = 0;

        for (PaymentOutbox outbox : unprocessedEvents) {
            try {
                publishToKafka(outbox);
                outbox.markAsProcessed();
                outboxRepository.save(outbox);
                successCount++;
                System.out.println(" Event gönderildi: " + outbox.getEventType()
                        + " → " + outbox.getAggregateId());
            } catch (Exception e) {
                outbox.markAsFailed(e.getMessage());
                outboxRepository.save(outbox);
                failCount++;
                System.err.println("  Event gönderilemedi: " + outbox.getEventType()
                        + " → " + outbox.getAggregateId()
                        + " (Retry: " + outbox.getRetryCount() + ")");
                System.err.println("      Hata: " + e.getMessage());
            }
        }

    }

    private void publishToKafka(PaymentOutbox outbox) throws Exception {
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
            System.err.println(" ALERT: " + deadLetterCount + " dead letter event var! Manuel müdahale gerekli.");
            
            List<PaymentOutbox> deadLetters = outboxRepository.findDeadLetterEvents();
            for (PaymentOutbox dl : deadLetters) {
                System.err.println(" Dead Letter: " + dl.getEventType()
                        + " → " + dl.getAggregateId() 
                        + " | Son Hata: " + dl.getLastError());
            }
        }
    }

    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void cleanupProcessedEvents() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(7);
        int deletedCount = outboxRepository.deleteProcessedBefore(threshold);
        if (deletedCount > 0) {
            System.out.println(" Temizlik: " + deletedCount + " eski outbox kaydı silindi");
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

