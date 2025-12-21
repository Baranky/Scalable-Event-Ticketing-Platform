package com.example.demo.service.impl;

import com.example.demo.service.LockManagementService;
import com.example.demo.service.SeatLockService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class LockManagementServiceImpl implements LockManagementService {

    private final SeatLockService seatLockService;

    public LockManagementServiceImpl(SeatLockService seatLockService) {
        this.seatLockService = seatLockService;
    }

    @Override
    public Map<String, Object> getLockedSeats(String stockId) {
        Map<String, Object> response = new HashMap<>();
        response.put("stockId", stockId);
        response.put("lockedSeats", seatLockService.getLockedSeats(stockId));
        response.put("totalLockedCount", seatLockService.getLockedCount(stockId));

        return response;
    }
}

