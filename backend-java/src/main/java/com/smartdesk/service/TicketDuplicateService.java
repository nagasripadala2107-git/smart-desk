package com.smartdesk.service;

import com.smartdesk.client.ai.*;
import com.smartdesk.dto.ticket.TicketDetailResponse;
import com.smartdesk.entity.Ticket;
import com.smartdesk.entity.TicketDuplicateMatch;
import com.smartdesk.repository.TicketDuplicateMatchRepository;
import com.smartdesk.repository.TicketRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TicketDuplicateService {

    private static final Logger log = LoggerFactory.getLogger(TicketDuplicateService.class);

    private final TicketDuplicateClient duplicateClient;
    private final TicketDuplicateMatchRepository duplicateMatchRepository;
    private final TicketRepository ticketRepository;

    @Value("${app.ai.duplicate.threshold:0.70}")
    private double similarityThreshold = 0.70;

    @Value("${app.ai.duplicate.max-matches:5}")
    private int maxMatches = 5;

    @Value("${app.ai.duplicate.max-candidates:50}")
    private int maxCandidates = 50;

    public TicketDuplicateService(
            TicketDuplicateClient duplicateClient,
            TicketDuplicateMatchRepository duplicateMatchRepository,
            TicketRepository ticketRepository
    ) {
        this.duplicateClient = duplicateClient;
        this.duplicateMatchRepository = duplicateMatchRepository;
        this.ticketRepository = ticketRepository;
    }

    public void setSimilarityThreshold(double similarityThreshold) {
        this.similarityThreshold = similarityThreshold;
    }

    public void setMaxMatches(int maxMatches) {
        this.maxMatches = maxMatches;
    }

    public void setMaxCandidates(int maxCandidates) {
        this.maxCandidates = maxCandidates;
    }

    /**
     * Executes duplicate ticket detection for a newly created ticket.
     * Compares the ticket text against bounded historical candidates.
     * Non-blocking and fails gracefully if AI microservice is unavailable.
     */
    @Transactional
    public List<TicketDuplicateMatch> detectAndPersistDuplicates(Ticket ticket) {
        if (ticket == null || ticket.getId() == null) {
            return Collections.emptyList();
        }

        String targetText = prepareTicketText(ticket.getSubject(), ticket.getDescription());
        if (targetText.isBlank()) {
            return Collections.emptyList();
        }

        try {
            // 1. Candidate Selection: Retrieve bounded candidates excluding the current ticket
            List<Ticket> candidateTickets = ticketRepository.findCandidatesForDuplicateDetection(
                    ticket.getId(),
                    PageRequest.of(0, maxCandidates)
            );

            if (candidateTickets.isEmpty()) {
                return Collections.emptyList();
            }

            Map<UUID, Ticket> candidateMap = candidateTickets.stream()
                    .collect(Collectors.toMap(Ticket::getId, t -> t, (existing, replacement) -> existing));

            List<DuplicateCandidateDto> candidateDtos = new ArrayList<>();
            for (Ticket candidate : candidateTickets) {
                // Application layer self-match exclusion
                if (candidate.getId().equals(ticket.getId())) {
                    continue;
                }
                String text = prepareTicketText(candidate.getSubject(), candidate.getDescription());
                if (!text.isBlank()) {
                    candidateDtos.add(new DuplicateCandidateDto(candidate.getId().toString(), text));
                }
            }

            if (candidateDtos.isEmpty()) {
                return Collections.emptyList();
            }

            // 2. Call Python AI Microservice
            DuplicateDetectionRequest request = new DuplicateDetectionRequest(targetText, candidateDtos);
            DuplicateDetectionResponse response = duplicateClient.analyze(request);

            if (response == null || response.matches() == null || response.matches().isEmpty()) {
                log.info("Duplicate detection completed for ticket {}: no matches returned above threshold.", ticket.getId());
                return Collections.emptyList();
            }

            String modelVersion = response.modelVersion() != null ? response.modelVersion() : "ticket-duplicate-v1";
            List<TicketDuplicateMatch> savedMatches = new ArrayList<>();

            // 3. Validate and Persist Matches
            int matchCount = 0;
            for (DuplicateMatchItemDto matchItem : response.matches()) {
                if (matchCount >= maxMatches) {
                    break;
                }

                if (matchItem.ticketId() == null || matchItem.ticketId().isBlank() || matchItem.similarity() == null) {
                    continue;
                }

                UUID candidateId;
                try {
                    candidateId = UUID.fromString(matchItem.ticketId());
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid candidate UUID returned from AI service: {}", matchItem.ticketId());
                    continue;
                }

                // Strict self-match prevention
                if (candidateId.equals(ticket.getId())) {
                    log.warn("Self-match detected and rejected for ticket {}", ticket.getId());
                    continue;
                }

                double similarity = matchItem.similarity();
                if (similarity < similarityThreshold) {
                    continue;
                }

                Ticket matchedTicket = candidateMap.get(candidateId);
                if (matchedTicket == null) {
                    matchedTicket = ticketRepository.findById(candidateId).orElse(null);
                }

                if (matchedTicket == null) {
                    log.warn("Matched ticket with ID {} not found in database.", candidateId);
                    continue;
                }

                // Prevent duplicate record insertion for the same analysis run
                if (duplicateMatchRepository.existsByTicketIdAndMatchedTicketId(ticket.getId(), candidateId)) {
                    continue;
                }

                BigDecimal similarityScore = BigDecimal.valueOf(Math.min(1.0, Math.max(0.0, similarity)))
                        .setScale(4, RoundingMode.HALF_UP);

                TicketDuplicateMatch matchEntity = new TicketDuplicateMatch(
                        ticket,
                        matchedTicket,
                        similarityScore,
                        modelVersion
                );

                savedMatches.add(duplicateMatchRepository.save(matchEntity));
                matchCount++;
            }

            log.info("Duplicate detection recorded {} potential duplicate matches for ticket {}",
                    savedMatches.size(), ticket.getId());
            return savedMatches;

        } catch (Exception ex) {
            // Graceful degradation: never disrupt ticket creation on AI failure
            log.warn("AI duplicate microservice unavailable or failed ({}). Continuing without duplicate detection.",
                    ex.getClass().getSimpleName() + ": " + ex.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Retrieves duplicate matches for a ticket formatted as DTOs for agent details view.
     */
    @Transactional(readOnly = true)
    public List<TicketDetailResponse.DuplicateMatchDto> getDuplicateMatchesForTicket(UUID ticketId) {
        if (ticketId == null) {
            return Collections.emptyList();
        }

        List<TicketDuplicateMatch> matches = duplicateMatchRepository.findByTicketIdOrderBySimilarityScoreDesc(ticketId);
        return matches.stream()
                .map(m -> new TicketDetailResponse.DuplicateMatchDto(
                        m.getMatchedTicket().getId(),
                        m.getMatchedTicket().getTicketNumber(),
                        m.getMatchedTicket().getSubject(),
                        m.getSimilarityScore(),
                        m.getModelVersion()
                ))
                .toList();
    }

    private String prepareTicketText(String subject, String description) {
        String s = subject != null ? subject.trim() : "";
        String d = description != null ? description.trim() : "";
        if (!s.isEmpty() && !d.isEmpty()) {
            return s + "\n" + d;
        }
        return !s.isEmpty() ? s : d;
    }
}
