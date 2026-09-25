package com.smartdesk.dto.customer;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CustomerResponse(
        UUID id,
        UUID userId,
        String customerCode,
        String companyName,
        String plan,
        String email,
        String firstName,
        String lastName,
        OffsetDateTime joinedAt
) {}
