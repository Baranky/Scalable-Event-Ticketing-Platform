package com.example.demo.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.demo.dto.EventReq;
import com.example.demo.dto.EventRes;
import com.example.demo.entity.Event;

@Service
public interface EventService {

    EventRes create(EventReq request);

    EventRes getById(String id);

    Event getEntityById(String id);

    List<EventRes> getAll();

    void delete(String id);

    EventRes publish(String id);

    EventRes openSales(String id);
}
