package com.example.demo.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.PriceCategoryReq;
import com.example.demo.dto.PriceCategoryRes;
import com.example.demo.service.PriceCategoryService;

@RestController
@RequestMapping("/api/price-categories")
public class PriceCategoryController {

    private final PriceCategoryService priceCategoryService;

    public PriceCategoryController(PriceCategoryService priceCategoryService) {
        this.priceCategoryService = priceCategoryService;
    }

    @PostMapping
    public ResponseEntity<PriceCategoryRes> create(@RequestBody PriceCategoryReq request) {
        PriceCategoryRes response = priceCategoryService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PriceCategoryRes> getById(@PathVariable String id) {
        PriceCategoryRes response = priceCategoryService.getById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<PriceCategoryRes>> getAll() {
        List<PriceCategoryRes> responses = priceCategoryService.getAll();
        return ResponseEntity.ok(responses);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        priceCategoryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
