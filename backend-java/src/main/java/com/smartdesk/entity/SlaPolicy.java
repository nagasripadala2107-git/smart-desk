package com.smartdesk.entity;

import com.smartdesk.entity.enums.TicketPriority;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "sla_policies")
public class SlaPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, unique = true, length = 20)
    private TicketPriority priority;

    @Column(name = "first_response_minutes", nullable = false)
    private int firstResponseMinutes;

    @Column(name = "resolution_minutes", nullable = false)
    private int resolutionMinutes;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    public SlaPolicy() {
    }

    public SlaPolicy(String name, TicketPriority priority, int firstResponseMinutes, int resolutionMinutes) {
        this.name = name;
        this.priority = priority;
        this.firstResponseMinutes = firstResponseMinutes;
        this.resolutionMinutes = resolutionMinutes;
        this.isActive = true;
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    public void onPreUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public TicketPriority getPriority() {
        return priority;
    }

    public void setPriority(TicketPriority priority) {
        this.priority = priority;
    }

    public int getFirstResponseMinutes() {
        return firstResponseMinutes;
    }

    public void setFirstResponseMinutes(int firstResponseMinutes) {
        this.firstResponseMinutes = firstResponseMinutes;
    }

    public int getResolutionMinutes() {
        return resolutionMinutes;
    }

    public void setResolutionMinutes(int resolutionMinutes) {
        this.resolutionMinutes = resolutionMinutes;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
