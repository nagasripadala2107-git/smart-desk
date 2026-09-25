package com.smartdesk.dto.customer;

import java.util.UUID;

public record CustomerProfileResponse(
        UUID customerId,
        UUID userId,
        String customerCode,
        String companyName,
        String plan,
        String email,
        String firstName,
        String lastName,
        String phone,
        String avatarUrl,
        long totalTickets,
        long activeTickets
) {}
