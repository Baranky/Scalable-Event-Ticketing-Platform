package com.example.demo.service;

import java.util.List;
import java.util.Map;

public interface SeatLockService {

    List<String> lockSeats(String stockId, List<String> seatLabels, String orderId);


    boolean lockGenericSeats(String stockId, int count, String orderId, int totalCount);

    void confirmGenericSale(String stockId, int count, String orderId);


    void unlockGenericSeats(String stockId, int count, String orderId);


    void unlockSeats(String stockId, List<String> seatLabels, String orderId);


    String isLocked(String stockId, String seatLabel);


    Map<String, String> getLockedSeats(String stockId);

    int getLockedCount(String stockId);

    int getLockedCountForOrder(String stockId, String orderId);
}
