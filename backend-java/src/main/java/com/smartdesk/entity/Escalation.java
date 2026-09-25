package com.smartdesk.entity;

import com.smartdesk.entity.enums.EscalationStatus;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "escalations")
public class Escalation {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_team_id")
    private Team fromTeam;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "to_team_id", nullable = false)
    private Team toTeam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_agent_id")
    private Agent fromAgent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_agent_id")
    private Agent toAgent;

    @Column(name = "level", nullable = false)
    private int level = 1;

    @Column(name = "reason", nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private EscalationStatus status = EscalationStatus.OPEN;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "resolved_at")
    private OffsetDateTime resolvedAt;

    public Escalation() {
    }

    public Escalation(Ticket ticket, Team fromTeam, Team toTeam, Agent fromAgent, Agent toAgent, int level, String reason) {
        this.ticket = ticket;
        this.fromTeam = fromTeam;
        this.toTeam = toTeam;
        this.fromAgent = fromAgent;
        this.toAgent = toAgent;
        this.level = level;
        this.reason = reason;
        this.status = EscalationStatus.OPEN;
        this.createdAt = OffsetDateTime.now();
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

    public Agent getFromAgent() {
        return fromAgent;
    }

    public void setFromAgent(Agent fromAgent) {
        this.fromAgent = fromAgent;
    }

    public Agent getToAgent() {
        return toAgent;
    }

    public void setToAgent(Agent toAgent) {
        this.toAgent = toAgent;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public EscalationStatus getStatus() {
        return status;
    }

    public void setStatus(EscalationStatus status) {
        this.status = status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(OffsetDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }
}
