package com.example.demo.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;


@Entity
@Table(name = "ticket_stocks", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"event_id", "price_category_id"})
})
public class TicketStock {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "event_id", nullable = false)
    private String eventId;

    @Column(name = "venue_id")
    private String venueId;

    @Column(name = "section_id")
    private String sectionId;

    @Column(name = "price_category_id", nullable = false)
    private String priceCategoryId;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(nullable = false)
    private String currency;

    @Column(name = "total_count", nullable = false)
    private int totalCount;

    @Column(name = "available_count", nullable = false)
    private int availableCount;

    @Column(name = "sold_count", nullable = false)
    private int soldCount = 0;

    @Column(name = "locked_count", nullable = false)
    private int lockedCount = 0;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }


    public TicketStock() {
    }


    public boolean lockTickets(int count) {
        if (availableCount >= count) {
            availableCount -= count;
            lockedCount += count;
            return true;
        }
        return false;
    }


    public boolean confirmSale(int count) {
        if (lockedCount >= count) {
            lockedCount -= count;
            soldCount += count;
            return true;
        }
        return false;
    }


    public boolean unlockTickets(int count) {
        if (lockedCount >= count) {
            lockedCount -= count;
            availableCount += count;
            return true;
        }
        return false;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getVenueId() {
        return venueId;
    }

    public void setVenueId(String venueId) {
        this.venueId = venueId;
    }

    public String getSectionId() {
        return sectionId;
    }

    public void setSectionId(String sectionId) {
        this.sectionId = sectionId;
    }

    public String getPriceCategoryId() {
        return priceCategoryId;
    }

    public void setPriceCategoryId(String priceCategoryId) {
        this.priceCategoryId = priceCategoryId;
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

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public int getAvailableCount() {
        return availableCount;
    }

    public void setAvailableCount(int availableCount) {
        this.availableCount = availableCount;
    }

    public int getSoldCount() {
        return soldCount;
    }

    public void setSoldCount(int soldCount) {
        this.soldCount = soldCount;
    }

    public int getLockedCount() {
        return lockedCount;
    }

    public void setLockedCount(int lockedCount) {
        this.lockedCount = lockedCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
