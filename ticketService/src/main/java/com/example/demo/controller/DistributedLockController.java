package com.example.demo.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.service.LockManagementService;

@RestController
@RequestMapping("/api/locks")
public class DistributedLockController {

    private final LockManagementService lockManagementService;

    public DistributedLockController(LockManagementService lockManagementService) {
        this.lockManagementService = lockManagementService;
    }

    @PostMapping("/seat")
    public ResponseEntity<Map<String, Object>> lockSeat(
            @RequestParam String eventId,
            @RequestParam String seatLabel,
            @RequestParam String orderId) {
        Map<String, Object> response = lockManagementService.lockSeat(eventId, seatLabel, orderId);

        if (response.containsKey("error")) {
            return ResponseEntity.status(500).body(response);
        }
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/seat")
    public ResponseEntity<Map<String, Object>> unlockSeat(
            @RequestParam String eventId,
            @RequestParam String seatLabel) {
        Map<String, Object> response = lockManagementService.unlockSeat(eventId, seatLabel);

        if (response.containsKey("error")) {
            return ResponseEntity.status(500).body(response);
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/seat/status")
    public ResponseEntity<Map<String, Object>> checkLockStatus(
            @RequestParam String eventId,
            @RequestParam String seatLabel) {
        Map<String, Object> response = lockManagementService.checkLockStatus(eventId, seatLabel);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/stock/{stockId}/seats")
    public ResponseEntity<Map<String, Object>> getLockedSeats(@PathVariable String stockId) {
        Map<String, Object> response = lockManagementService.getLockedSeats(stockId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/test-race")
    public ResponseEntity<Map<String, Object>> testRaceCondition(
            @RequestParam String eventId,
            @RequestParam String seatLabel,
            @RequestParam(defaultValue = "100") int threads) {
        Map<String, Object> response = lockManagementService.testRaceCondition(eventId, seatLabel, threads);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/test-fair")
    public ResponseEntity<Map<String, Object>> testFairLock(
            @RequestParam String eventId,
            @RequestParam String seatLabel,
            @RequestParam(defaultValue = "10") int threads) {
        Map<String, Object> response = lockManagementService.testFairLock(eventId, seatLabel, threads);
        return ResponseEntity.ok(response);
    }
}
