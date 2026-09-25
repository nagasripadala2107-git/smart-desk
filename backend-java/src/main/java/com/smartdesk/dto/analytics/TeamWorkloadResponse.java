package com.smartdesk.dto.analytics;

import java.util.UUID;

public record TeamWorkloadResponse(
        UUID teamId,
        String teamName,
        long activeTickets,
        long resolvedClosedTickets,
        long escalatedTickets
) {}
