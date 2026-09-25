package com.smartdesk.dto.ticket;

import com.smartdesk.entity.enums.TicketPriority;
import com.smartdesk.entity.enums.TicketStatus;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record UpdateTicketRequest(
        @Size(max = 255, message = "Subject cannot exceed 255 characters")
        String subject,

        @Size(max = 10000, message = "Description cannot exceed 10000 characters")
        String description,

        TicketPriority priority,
        TicketStatus status,
        UUID categoryId,
        UUID assignedAgentId,
        UUID assignedTeamId,

        @Size(max = 10000, message = "Update reason cannot exceed 10000 characters")
        String updateReason
) {}
