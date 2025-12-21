package com.example.demo.service;

import java.util.Map;

public interface LockManagementService {

    Map<String, Object> getLockedSeats(String stockId);
}

