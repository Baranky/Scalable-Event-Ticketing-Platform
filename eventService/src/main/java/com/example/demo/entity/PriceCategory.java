package com.example.demo.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import java.math.BigDecimal;

@Entity
public class PriceCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private BigDecimal price;
    private String currency;

    private int totalAllocation;

    @ManyToOne
    private Event event;

    @ManyToOne
    private Section section;

    public PriceCategory() {
    }

    public PriceCategory(String id, BigDecimal price, String currency, int totalAllocation, Event event, Section section) {
        this.id = id;
        this.price = price;
        this.currency = currency;
        this.totalAllocation = totalAllocation;
        this.event = event;
        this.section = section;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public int getTotalAllocation() {
        return totalAllocation;
    }

    public void setTotalAllocation(int totalAllocation) {
        this.totalAllocation = totalAllocation;
    }

    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    public Section getSection() {
        return section;
    }

    public void setSection(Section section) {
        this.section = section;
    }
}
