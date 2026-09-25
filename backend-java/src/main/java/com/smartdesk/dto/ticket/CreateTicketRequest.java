package com.smartdesk.dto.ticket;

import com.smartdesk.entity.enums.TicketPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CreateTicketRequest(
        @NotBlank(message = "Subject is required")
        @Size(max = 255, message = "Subject cannot exceed 255 characters")
        String subject,

        @NotBlank(message = "Description is required")
        @Size(max = 10000, message = "Description cannot exceed 10000 characters")
        String description,

        @NotNull(message = "Priority is required")
        TicketPriority priority,

        UUID categoryId
) {}
