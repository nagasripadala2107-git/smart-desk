package com.smartdesk.dto.ticket;

import com.smartdesk.entity.enums.TicketPriority;
import com.smartdesk.entity.enums.TicketStatus;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record TicketResponse(
        UUID id,
        String ticketNumber,
        UUID customerId,
        String customerName,
        String customerCode,
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
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        OffsetDateTime resolvedAt,
        OffsetDateTime closedAt
) {}
