package com.example.demo.controller;

import com.example.demo.model.PaymentOutbox;
import com.example.demo.repository.PaymentOutboxRepository;
import com.example.demo.scheduler.OutboxProcessor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/api/outbox")
public class OutboxController {

    private final PaymentOutboxRepository outboxRepository;
    private final OutboxProcessor outboxProcessor;

    public OutboxController(PaymentOutboxRepository outboxRepository,
                           OutboxProcessor outboxProcessor) {
        this.outboxRepository = outboxRepository;
        this.outboxProcessor = outboxProcessor;
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        OutboxProcessor.OutboxStats stats = outboxProcessor.getStats();
        
        Map<String, Object> response = new HashMap<>();
        response.put("pending", stats.pending());
        response.put("processed", stats.processed());
        response.put("deadLetters", stats.deadLetters());
        response.put("total", stats.pending() + stats.processed() + stats.deadLetters());
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/pending")
    public ResponseEntity<List<PaymentOutbox>> getPendingEvents() {
        return ResponseEntity.ok(outboxRepository.findUnprocessedOutboxEvents());
    }

    @GetMapping("/dead-letters")
    public ResponseEntity<List<PaymentOutbox>> getDeadLetters() {
        return ResponseEntity.ok(outboxRepository.findDeadLetterEvents());
    }
    @PostMapping("/process-now")
    public ResponseEntity<Map<String, Object>> processNow() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            long beforePending = outboxRepository.countByProcessedFalse();
            outboxProcessor.processOutbox();
            long afterPending = outboxRepository.countByProcessedFalse();
            
            response.put("success", true);
            response.put("processed", beforePending - afterPending);
            response.put("remaining", afterPending);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @PostMapping("/retry/{id}")
    public ResponseEntity<Map<String, Object>> retryDeadLetter(@PathVariable String id) {
        Map<String, Object> response = new HashMap<>();
        
        PaymentOutbox outbox = outboxRepository.findById(id).orElse(null);
        if (outbox == null) {
            response.put("error", "Outbox event not found");
            return ResponseEntity.notFound().build();
        }
        
        outbox.setRetryCount(0);
        outbox.setLastError(null);
        outboxRepository.save(outbox);
        
        response.put("success", true);
        response.put("message", "Event retry için sıfırlandı, scheduler bir sonraki çalışmada işleyecek");
        response.put("outboxId", id);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/history/{aggregateId}")
    public ResponseEntity<List<PaymentOutbox>> getOutboxHistory(@PathVariable String aggregateId) {
        return ResponseEntity.ok(
                outboxRepository.findByAggregateIdOrderByCreatedAtAsc(aggregateId)
        );
    }
}

