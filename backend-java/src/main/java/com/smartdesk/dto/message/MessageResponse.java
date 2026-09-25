package com.smartdesk.dto.message;

import com.smartdesk.entity.enums.UserRole;
import java.time.OffsetDateTime;
import java.util.UUID;

public record MessageResponse(
        UUID id,
        UUID ticketId,
        UUID senderId,
        String senderName,
        UserRole senderRole,
        String message,
        boolean isInternal,
        OffsetDateTime createdAt
) {}
