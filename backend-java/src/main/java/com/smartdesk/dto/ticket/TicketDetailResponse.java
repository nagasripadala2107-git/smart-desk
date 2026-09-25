package com.smartdesk.dto.ticket;

import com.smartdesk.dto.message.MessageResponse;
import com.smartdesk.entity.enums.EventType;
import com.smartdesk.entity.enums.TicketPriority;
import com.smartdesk.entity.enums.TicketStatus;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record TicketDetailResponse(
        UUID id,
        String ticketNumber,
        UUID customerId,
        String customerName,
        String customerCode,
        String companyName,
        UUID categoryId,
        String categoryName,
        UUID assignedAgentId,
        String assignedAgentName,
        UUID assignedTeamId,
        String assignedTeamName,
        String subject,
        String description,
        TicketPriority priority,
        TicketStatus status,
        String aiCategory,
        BigDecimal aiConfidence,
        String aiModelVersion,
        String sentiment,
        BigDecimal sentimentConfidence,
        String tone,
        String sentimentModelVersion,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        OffsetDateTime resolvedAt,
        OffsetDateTime closedAt,
        List<MessageResponse> messages,
        List<TicketEventDto> events,
        List<DuplicateMatchDto> duplicateMatches
) {
    public TicketDetailResponse(
            UUID id,
            String ticketNumber,
            UUID customerId,
            String customerName,
            String customerCode,
            String companyName,
            UUID categoryId,
            String categoryName,
            UUID assignedAgentId,
            String assignedAgentName,
            UUID assignedTeamId,
            String assignedTeamName,
            String subject,
            String description,
            TicketPriority priority,
            TicketStatus status,
            String aiCategory,
            BigDecimal aiConfidence,
            String aiModelVersion,
            String sentiment,
            BigDecimal sentimentConfidence,
            String tone,
            String sentimentModelVersion,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt,
            OffsetDateTime resolvedAt,
            OffsetDateTime closedAt,
            List<MessageResponse> messages,
            List<TicketEventDto> events
    ) {
        this(id, ticketNumber, customerId, customerName, customerCode, companyName,
                categoryId, categoryName, assignedAgentId, assignedAgentName,
                assignedTeamId, assignedTeamName, subject, description, priority, status,
                aiCategory, aiConfidence, aiModelVersion, sentiment, sentimentConfidence,
                tone, sentimentModelVersion, createdAt, updatedAt, resolvedAt, closedAt,
                messages, events, null);
    }

    public record TicketEventDto(
            UUID id,
            String actorName,
            EventType eventType,
            String oldValue,
            String newValue,
            String metadata,
            OffsetDateTime createdAt
    ) {}

    public record DuplicateMatchDto(
            UUID ticketId,
            String ticketNumber,
            String subject,
            BigDecimal similarityScore,
            String modelVersion
    ) {}
}
