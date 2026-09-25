package com.smartdesk.service;

import com.smartdesk.client.ai.SentimentRequest;
import com.smartdesk.client.ai.SentimentResponse;
import com.smartdesk.client.ai.TicketSentimentClient;
import com.smartdesk.entity.Ticket;
import com.smartdesk.entity.TicketMessage;
import com.smartdesk.entity.TicketSentimentAnalysis;
import com.smartdesk.entity.enums.CustomerTone;
import com.smartdesk.entity.enums.SentimentType;
import com.smartdesk.repository.TicketSentimentAnalysisRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import java.util.UUID;

@Service
public class TicketSentimentService {

    private static final Logger log = LoggerFactory.getLogger(TicketSentimentService.class);

    private final TicketSentimentClient sentimentClient;
    private final TicketSentimentAnalysisRepository sentimentRepository;

    public TicketSentimentService(TicketSentimentClient sentimentClient,
                                  TicketSentimentAnalysisRepository sentimentRepository) {
        this.sentimentClient = sentimentClient;
        this.sentimentRepository = sentimentRepository;
    }

    /**
     * Analyzes and persists sentiment for a ticket's initial subject and description.
     */
    @Transactional
    public Optional<TicketSentimentAnalysis> analyzeAndPersistTicketSentiment(Ticket ticket, String text) {
        return analyzeAndPersist(ticket, null, text);
    }

    /**
     * Analyzes and persists sentiment for an incoming customer message.
     * Enforces ticket/message relationship integrity.
     */
    @Transactional
    public Optional<TicketSentimentAnalysis> analyzeAndPersistMessageSentiment(Ticket ticket,
                                                                               TicketMessage message,
                                                                               String text) {
        return analyzeAndPersist(ticket, message, text);
    }

    /**
     * Core sentiment processing method.
     * Enforces ticket-message relationship integrity and graceful degradation.
     */
    @Transactional
    public Optional<TicketSentimentAnalysis> analyzeAndPersist(Ticket ticket,
                                                               TicketMessage message,
                                                               String text) {
        if (ticket == null || text == null || text.isBlank()) {
            return Optional.empty();
        }

        // Application-layer relationship integrity enforcement
        if (message != null) {
            if (message.getTicket() == null || message.getTicket().getId() == null || !message.getTicket().getId().equals(ticket.getId())) {
                throw new IllegalArgumentException(
                        String.format("Relationship integrity violation: Message %s does not belong to Ticket %s",
                                message.getId(), ticket.getId())
                );
            }
        }

        try {
            SentimentRequest request = new SentimentRequest(text);
            SentimentResponse response = sentimentClient.analyze(request);

            if (response == null || response.sentiment() == null || response.sentiment().isBlank()) {
                log.warn("AI sentiment analysis returned an empty response. Skipping sentiment persistence.");
                return Optional.empty();
            }

            SentimentType sentiment = parseSentiment(response.sentiment());
            CustomerTone tone = parseTone(response.tone());
            BigDecimal confidence = BigDecimal.valueOf(response.confidence() != null ? response.confidence() : 0.0)
                    .setScale(4, RoundingMode.HALF_UP);
            String modelVersion = response.modelVersion() != null ? response.modelVersion() : "unknown";
            String textHash = computeSha256Hash(text);

            TicketSentimentAnalysis analysis = new TicketSentimentAnalysis(
                    ticket,
                    message,
                    sentiment,
                    confidence,
                    tone,
                    modelVersion,
                    textHash
            );

            TicketSentimentAnalysis saved = sentimentRepository.save(analysis);
            log.info("Sentiment analyzed and recorded for ticket {}: sentiment={}, tone={}, confidence={}",
                    ticket.getId(), sentiment, tone, confidence);
            return Optional.of(saved);

        } catch (Exception ex) {
            // Graceful degradation: never fail business workflow on AI microservice outage
            log.warn("AI sentiment microservice unavailable or failed ({}). Continuing without sentiment analysis.",
                    ex.getClass().getSimpleName() + ": " + ex.getMessage());
            return Optional.empty();
        }
    }

    @Transactional(readOnly = true)
    public Optional<TicketSentimentAnalysis> getLatestSentimentForTicket(UUID ticketId) {
        return sentimentRepository.findFirstByTicketIdOrderByCreatedAtDesc(ticketId);
    }

    @Transactional(readOnly = true)
    public Optional<TicketSentimentAnalysis> getSentimentForMessage(UUID messageId) {
        return sentimentRepository.findFirstByMessageId(messageId);
    }

    private SentimentType parseSentiment(String sentimentStr) {
        try {
            return SentimentType.valueOf(sentimentStr.toUpperCase());
        } catch (Exception e) {
            log.warn("Unrecognized sentiment value '{}', defaulting to NEUTRAL", sentimentStr);
            return SentimentType.NEUTRAL;
        }
    }

    private CustomerTone parseTone(String toneStr) {
        try {
            return CustomerTone.valueOf(toneStr.toUpperCase());
        } catch (Exception e) {
            log.warn("Unrecognized tone value '{}', defaulting to NEUTRAL", toneStr);
            return CustomerTone.NEUTRAL;
        }
    }

    private String computeSha256Hash(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
