package com.smartdesk.dto.escalation;

import com.smartdesk.entity.enums.EscalationStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

public record EscalationResponse(
        UUID id,
        UUID ticketId,
        String ticketNumber,
        UUID fromTeamId,
        String fromTeamName,
        UUID toTeamId,
        String toTeamName,
        UUID fromAgentId,
        String fromAgentName,
        UUID toAgentId,
        String toAgentName,
        int level,
        String reason,
        EscalationStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime resolvedAt
) {}
