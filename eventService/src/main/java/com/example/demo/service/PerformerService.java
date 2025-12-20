package com.example.demo.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.demo.dto.PerformerReq;
import com.example.demo.dto.PerformerRes;

@Service
public interface PerformerService {

    PerformerRes create(PerformerReq request);

    PerformerRes getById(String id);

    List<PerformerRes> getAll();

    void delete(String id);
}
