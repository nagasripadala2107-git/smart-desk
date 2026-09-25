package com.smartdesk.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "escalation_rules")
public class EscalationRule {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "from_team_id", nullable = false)
    private Team fromTeam;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "to_team_id", nullable = false)
    private Team toTeam;

    @Column(name = "trigger_type", nullable = false, length = 20)
    private String triggerType;

    @Column(name = "trigger_value", nullable = false, length = 100)
    private String triggerValue;

    @Column(name = "escalation_level", nullable = false)
    private int escalationLevel = 1;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    public EscalationRule() {
    }

    public EscalationRule(String name, Team fromTeam, Team toTeam, String triggerType, String triggerValue, int escalationLevel) {
        this.name = name;
        this.fromTeam = fromTeam;
        this.toTeam = toTeam;
        this.triggerType = triggerType;
        this.triggerValue = triggerValue;
        this.escalationLevel = escalationLevel;
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

    public Team getFromTeam() {
        return fromTeam;
    }

    public void setFromTeam(Team fromTeam) {
        this.fromTeam = fromTeam;
    }

    public Team getToTeam() {
        return toTeam;
    }

    public void setToTeam(Team toTeam) {
        this.toTeam = toTeam;
    }

    public String getTriggerType() {
        return triggerType;
    }

    public void setTriggerType(String triggerType) {
        this.triggerType = triggerType;
    }

    public String getTriggerValue() {
        return triggerValue;
    }

    public void setTriggerValue(String triggerValue) {
        this.triggerValue = triggerValue;
    }

    public int getEscalationLevel() {
        return escalationLevel;
    }

    public void setEscalationLevel(int escalationLevel) {
        this.escalationLevel = escalationLevel;
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
