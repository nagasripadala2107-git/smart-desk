package com.smartdesk.dto.analytics;

import java.util.UUID;

public record AgentPerformanceResponse(
        UUID agentId,
        String agentName,
        String employeeCode,
        String teamName,
        long assignedTickets,
        long openTickets,
        long resolvedTickets,
        Double avgResolutionMinutes
) {}
