package com.example.demo.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.demo.dto.PriceCategoryReq;
import com.example.demo.dto.PriceCategoryRes;

@Service
public interface PriceCategoryService {

    PriceCategoryRes create(PriceCategoryReq request);

    PriceCategoryRes getById(String id);

    List<PriceCategoryRes> getAll();

    void delete(String id);
}
