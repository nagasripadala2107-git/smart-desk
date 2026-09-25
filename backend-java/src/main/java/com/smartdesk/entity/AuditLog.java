package com.smartdesk.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_user_id")
    private User actor;

    @Column(name = "action", nullable = false, length = 100)
    private String action;

    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType;

    @Column(name = "entity_id", nullable = false, length = 100)
    private String entityId;

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "old_data", columnDefinition = "jsonb")
    private String oldData;

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "new_data", columnDefinition = "jsonb")
    private String newData;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public AuditLog() {
    }

    public AuditLog(User actor, String action, String entityType, String entityId, String oldData, String newData, String ipAddress, String userAgent) {
        this.actor = actor;
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.oldData = formatJson(oldData);
        this.newData = formatJson(newData);
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getActor() {
        return actor;
    }

    public void setActor(User actor) {
        this.actor = actor;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public String getOldData() {
        return oldData;
    }

    public void setOldData(String oldData) {
        this.oldData = formatJson(oldData);
    }

    public String getNewData() {
        return newData;
    }

    public void setNewData(String newData) {
        this.newData = formatJson(newData);
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
