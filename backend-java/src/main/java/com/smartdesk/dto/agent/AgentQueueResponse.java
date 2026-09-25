package com.smartdesk.dto.agent;

import com.smartdesk.dto.ticket.TicketSummaryResponse;
import java.util.List;
import java.util.UUID;

public record AgentQueueResponse(
        UUID agentId,
        String employeeCode,
        String agentName,
        UUID teamId,
        String teamName,
        long assignedCount,
        List<TicketSummaryResponse> assignedTickets,
        List<TicketSummaryResponse> teamQueueTickets
) {}
