package com.smartdesk.repository;

import com.smartdesk.entity.TicketSentimentAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TicketSentimentAnalysisRepository extends JpaRepository<TicketSentimentAnalysis, UUID> {
    Optional<TicketSentimentAnalysis> findFirstByTicketIdOrderByCreatedAtDesc(UUID ticketId);
    Optional<TicketSentimentAnalysis> findFirstByMessageId(UUID messageId);
    List<TicketSentimentAnalysis> findByTicketIdOrderByCreatedAtAsc(UUID ticketId);
}
