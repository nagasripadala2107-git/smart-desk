package com.smartdesk.dto.ticket;

import com.smartdesk.entity.enums.TicketPriority;
import com.smartdesk.entity.enums.TicketStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

public record TicketSummaryResponse(
        UUID id,
        String ticketNumber,
        String subject,
        String customerName,
        String categoryName,
        TicketPriority priority,
        TicketStatus status,
        String assignedAgentName,
        String assignedTeamName,
        OffsetDateTime createdAt
) {}
