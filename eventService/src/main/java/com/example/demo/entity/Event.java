package com.example.demo.entity;

import com.example.demo.enums.EventStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Entity
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    private String name;
    private String description;
    private String imageUrl;

    private LocalDateTime eventDate;
    private LocalDateTime doorsOpen;
    private LocalDateTime salesStartDate;
    private LocalDateTime salesEndDate;

    @Enumerated(EnumType.STRING)
    private EventStatus status;

    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> attributes;

    @ManyToOne
    private Venue venue;

    @OneToMany(mappedBy = "event")
    private List<PriceCategory> priceCategories;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public LocalDateTime getEventDate() {
        return eventDate;
    }

    public void setEventDate(LocalDateTime eventDate) {
        this.eventDate = eventDate;
    }

    public LocalDateTime getDoorsOpen() {
        return doorsOpen;
    }

    public void setDoorsOpen(LocalDateTime doorsOpen) {
        this.doorsOpen = doorsOpen;
    }

    public LocalDateTime getSalesStartDate() {
        return salesStartDate;
    }

    public void setSalesStartDate(LocalDateTime salesStartDate) {
        this.salesStartDate = salesStartDate;
    }

    public LocalDateTime getSalesEndDate() {
        return salesEndDate;
    }

    public void setSalesEndDate(LocalDateTime salesEndDate) {
        this.salesEndDate = salesEndDate;
    }

    public EventStatus getStatus() {
        return status;
    }

    public void setStatus(EventStatus status) {
        this.status = status;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    public Venue getVenue() {
        return venue;
    }

    public void setVenue(Venue venue) {
        this.venue = venue;
    }

    public List<PriceCategory> getPriceCategories() {
        return priceCategories;
    }

    public void setPriceCategories(List<PriceCategory> priceCategories) {
        this.priceCategories = priceCategories;
    }
}
