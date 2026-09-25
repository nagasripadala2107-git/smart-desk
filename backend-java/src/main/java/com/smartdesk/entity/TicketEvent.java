package com.smartdesk.entity;

import com.smartdesk.entity.enums.EventType;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "ticket_events")
public class TicketEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_user_id")
    private User actor;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private EventType eventType;

    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue;

    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadata;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public TicketEvent() {
    }

    public TicketEvent(Ticket ticket, User actor, EventType eventType, String oldValue, String newValue, String metadata) {
        this.ticket = ticket;
        this.actor = actor;
        this.eventType = eventType;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.metadata = formatJson(metadata);
        this.createdAt = OffsetDateTime.now();
    }

    private static String formatJson(String raw) {
        if (raw == null) return null;
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) return null;
        if ((trimmed.startsWith("{") && trimmed.endsWith("}")) ||
            (trimmed.startsWith("[") && trimmed.endsWith("]")) ||
            (trimmed.startsWith("\"") && trimmed.endsWith("\""))) {
            return trimmed;
        }
        return "\"" + trimmed.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Ticket getTicket() {
        return ticket;
    }

    public void setTicket(Ticket ticket) {
        this.ticket = ticket;
    }

    public User getActor() {
        return actor;
    }

    public void setActor(User actor) {
        this.actor = actor;
    }

    public EventType getEventType() {
        return eventType;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    public String getOldValue() {
        return oldValue;
    }

    public void setOldValue(String oldValue) {
        this.oldValue = oldValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public void setNewValue(String newValue) {
        this.newValue = newValue;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = formatJson(metadata);
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
