package com.smartdesk.dto.team;

import java.util.UUID;

public record TeamResponse(
        UUID id,
        String name,
        String description,
        boolean isActive,
        int agentCount
) {}
