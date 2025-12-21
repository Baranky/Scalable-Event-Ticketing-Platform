package com.example.demo.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.entity.Event;

@Repository
public interface EventRepository extends JpaRepository<Event, String> {

    @EntityGraph(attributePaths = {"venue", "priceCategories", "priceCategories.section"})
    @Query("SELECT e FROM Event e WHERE e.id = :id")
    Optional<Event> findByIdWithRelations(@Param("id") String id);
}
