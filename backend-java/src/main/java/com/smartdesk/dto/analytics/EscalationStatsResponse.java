package com.smartdesk.dto.analytics;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record EscalationStatsResponse(
        long totalEscalations,
        Map<Integer, Long> byLevel,
        Map<String, Long> byTeam,
        List<EscalationSummaryDto> recentEscalations
) {
    public record EscalationSummaryDto(
            UUID id,
            String ticketNumber,
            int level,
            String fromTeamName,
            String toTeamName,
            String reason,
            String status,
            OffsetDateTime createdAt
    ) {}
}
