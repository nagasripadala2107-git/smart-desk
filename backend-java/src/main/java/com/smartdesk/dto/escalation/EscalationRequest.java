package com.smartdesk.dto.escalation;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record EscalationRequest(
        @NotNull(message = "Target team ID is required")
        UUID targetTeamId,

        UUID targetAgentId,

        @Min(value = 1, message = "Escalation level must be at least 1")
        @Max(value = 5, message = "Escalation level cannot exceed 5")
        int level,

        @NotBlank(message = "Escalation reason is required")
        @Size(max = 1000, message = "Escalation reason cannot exceed 1000 characters")
        String reason
) {}
