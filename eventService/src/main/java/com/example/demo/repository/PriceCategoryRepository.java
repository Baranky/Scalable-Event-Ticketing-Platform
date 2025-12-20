package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.entity.PriceCategory;

@Repository
public interface PriceCategoryRepository extends JpaRepository<PriceCategory, String> {
}
