package com.smartdesk.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "ticket_duplicate_matches",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_ticket_duplicate_pair", columnNames = {"ticket_id", "matched_ticket_id"})
        }
)
public class TicketDuplicateMatch {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "matched_ticket_id", nullable = false)
    private Ticket matchedTicket;

    @Column(name = "similarity_score", nullable = false, precision = 5, scale = 4)
    private BigDecimal similarityScore;

    @Column(name = "model_version", nullable = false, length = 50)
    private String modelVersion;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public TicketDuplicateMatch() {
    }

    public TicketDuplicateMatch(Ticket ticket, Ticket matchedTicket, BigDecimal similarityScore, String modelVersion) {
        validateNotSelfMatch(ticket, matchedTicket);
        this.ticket = ticket;
        this.matchedTicket = matchedTicket;
        this.similarityScore = similarityScore;
        this.modelVersion = modelVersion;
        this.createdAt = OffsetDateTime.now();
    }

    private void validateNotSelfMatch(Ticket ticket, Ticket matchedTicket) {
        if (ticket != null && matchedTicket != null) {
            if (ticket.getId() != null && matchedTicket.getId() != null && ticket.getId().equals(matchedTicket.getId())) {
                throw new IllegalArgumentException(
                        String.format("Self-match violation: Ticket %s cannot be matched as a duplicate of itself.",
                                ticket.getId())
                );
            }
        }
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
        validateNotSelfMatch(ticket, this.matchedTicket);
        this.ticket = ticket;
    }

    public Ticket getMatchedTicket() {
        return matchedTicket;
    }

    public void setMatchedTicket(Ticket matchedTicket) {
        validateNotSelfMatch(this.ticket, matchedTicket);
        this.matchedTicket = matchedTicket;
    }

    public BigDecimal getSimilarityScore() {
        return similarityScore;
    }

    public void setSimilarityScore(BigDecimal similarityScore) {
        this.similarityScore = similarityScore;
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public void setModelVersion(String modelVersion) {
        this.modelVersion = modelVersion;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
