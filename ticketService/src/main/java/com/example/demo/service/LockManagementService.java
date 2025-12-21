package com.example.demo.service;

import java.util.Map;

public interface LockManagementService {

    Map<String, Object> lockSeat(String eventId, String seatLabel, String orderId);

    Map<String, Object> unlockSeat(String eventId, String seatLabel);

    Map<String, Object> checkLockStatus(String eventId, String seatLabel);

    Map<String, Object> getLockedSeats(String stockId);

    Map<String, Object> testRaceCondition(String eventId, String seatLabel, int threads);

    Map<String, Object> testFairLock(String eventId, String seatLabel, int threads);
}

