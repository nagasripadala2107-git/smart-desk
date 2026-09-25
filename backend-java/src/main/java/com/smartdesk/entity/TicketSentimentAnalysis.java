package com.smartdesk.entity;

import com.smartdesk.entity.enums.CustomerTone;
import com.smartdesk.entity.enums.SentimentType;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "ticket_sentiment_analysis")
public class TicketSentimentAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id")
    private TicketMessage message;

    @Enumerated(EnumType.STRING)
    @Column(name = "sentiment", nullable = false, length = 20)
    private SentimentType sentiment;

    @Column(name = "confidence", nullable = false, precision = 5, scale = 4)
    private BigDecimal confidence;

    @Enumerated(EnumType.STRING)
    @Column(name = "tone", nullable = false, length = 30)
    private CustomerTone tone;

    @Column(name = "model_version", nullable = false, length = 50)
    private String modelVersion;

    @Column(name = "analyzed_text_hash", nullable = false, length = 64)
    private String analyzedTextHash;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public TicketSentimentAnalysis() {
    }

    public TicketSentimentAnalysis(Ticket ticket, TicketMessage message, SentimentType sentiment,
                                   BigDecimal confidence, CustomerTone tone, String modelVersion,
                                   String analyzedTextHash) {
        this.ticket = ticket;
        this.message = message;
        this.sentiment = sentiment;
        this.confidence = confidence;
        this.tone = tone;
        this.modelVersion = modelVersion;
        this.analyzedTextHash = analyzedTextHash;
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

    public TicketMessage getMessage() {
        return message;
    }

    public void setMessage(TicketMessage message) {
        this.message = message;
    }

    public SentimentType getSentiment() {
        return sentiment;
    }

    public void setSentiment(SentimentType sentiment) {
        this.sentiment = sentiment;
    }

    public BigDecimal getConfidence() {
        return confidence;
    }

    public void setConfidence(BigDecimal confidence) {
        this.confidence = confidence;
    }

    public CustomerTone getTone() {
        return tone;
    }

    public void setTone(CustomerTone tone) {
        this.tone = tone;
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public void setModelVersion(String modelVersion) {
        this.modelVersion = modelVersion;
    }

    public String getAnalyzedTextHash() {
        return analyzedTextHash;
    }

    public void setAnalyzedTextHash(String analyzedTextHash) {
        this.analyzedTextHash = analyzedTextHash;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
