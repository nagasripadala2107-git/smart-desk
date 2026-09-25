package com.smartdesk.entity;

import com.smartdesk.entity.enums.AgentAvailability;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "agents")
public class Agent {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    @Column(name = "employee_code", nullable = false, unique = true, length = 50)
    private String employeeCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "availability_status", nullable = false, length = 20)
    private AgentAvailability availabilityStatus = AgentAvailability.OFFLINE;

    @Column(name = "skills", columnDefinition = "TEXT")
    private String skills;

    @Column(name = "max_active_tickets", nullable = false)
    private int maxActiveTickets = 5;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    public Agent() {
    }

    public Agent(User user, Team team, String employeeCode, AgentAvailability availabilityStatus, String skills, int maxActiveTickets) {
        this.user = user;
        this.team = team;
        this.employeeCode = employeeCode;
        this.availabilityStatus = availabilityStatus != null ? availabilityStatus : AgentAvailability.OFFLINE;
        this.skills = skills;
        this.maxActiveTickets = maxActiveTickets;
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

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Team getTeam() {
        return team;
    }

    public void setTeam(Team team) {
        this.team = team;
    }

    public String getEmployeeCode() {
        return employeeCode;
    }

    public void setEmployeeCode(String employeeCode) {
        this.employeeCode = employeeCode;
    }

    public AgentAvailability getAvailabilityStatus() {
        return availabilityStatus;
    }

    public void setAvailabilityStatus(AgentAvailability availabilityStatus) {
        this.availabilityStatus = availabilityStatus;
    }

    public String getSkills() {
        return skills;
    }

    public void setSkills(String skills) {
        this.skills = skills;
    }

    public int getMaxActiveTickets() {
        return maxActiveTickets;
    }

    public void setMaxActiveTickets(int maxActiveTickets) {
        this.maxActiveTickets = maxActiveTickets;
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
