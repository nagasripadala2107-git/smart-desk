package com.smartdesk.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "ticket_assignments")
public class TicketAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id")
    private Agent agent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assigned_by_user_id", nullable = false)
    private User assignedByUser;

    @Column(name = "assignment_reason", columnDefinition = "TEXT")
    private String assignmentReason;

    @Column(name = "assigned_at", nullable = false, updatable = false)
    private OffsetDateTime assignedAt = OffsetDateTime.now();

    @Column(name = "unassigned_at")
    private OffsetDateTime unassignedAt;

    public TicketAssignment() {
    }

    public TicketAssignment(Ticket ticket, Agent agent, Team team, User assignedByUser, String assignmentReason) {
        this.ticket = ticket;
        this.agent = agent;
        this.team = team;
        this.assignedByUser = assignedByUser;
        this.assignmentReason = assignmentReason;
        this.assignedAt = OffsetDateTime.now();
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

    public Agent getAgent() {
        return agent;
    }

    public void setAgent(Agent agent) {
        this.agent = agent;
    }

    public Team getTeam() {
        return team;
    }

    public void setTeam(Team team) {
        this.team = team;
    }

    public User getAssignedByUser() {
        return assignedByUser;
    }

    public void setAssignedByUser(User assignedByUser) {
        this.assignedByUser = assignedByUser;
    }

    public String getAssignmentReason() {
        return assignmentReason;
    }

    public void setAssignmentReason(String assignmentReason) {
        this.assignmentReason = assignmentReason;
    }

    public OffsetDateTime getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(OffsetDateTime assignedAt) {
        this.assignedAt = assignedAt;
    }

    public OffsetDateTime getUnassignedAt() {
        return unassignedAt;
    }

    public void setUnassignedAt(OffsetDateTime unassignedAt) {
        this.unassignedAt = unassignedAt;
    }
}
