package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.entity.Performer;

@Repository
public interface PerformerRepository extends JpaRepository<Performer, String> {
}
