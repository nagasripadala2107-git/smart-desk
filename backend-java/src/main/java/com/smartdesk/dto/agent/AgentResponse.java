package com.smartdesk.dto.agent;

import com.smartdesk.entity.enums.AgentAvailability;
import java.util.UUID;

public record AgentResponse(
        UUID id,
        UUID userId,
        String employeeCode,
        String firstName,
        String lastName,
        String email,
        UUID teamId,
        String teamName,
        AgentAvailability availabilityStatus,
        String skills,
        int maxActiveTickets,
        long currentActiveTickets
) {}
