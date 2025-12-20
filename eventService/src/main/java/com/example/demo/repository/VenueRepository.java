package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.entity.Venue;

@Repository
public interface VenueRepository extends JpaRepository<Venue, String> {
}
