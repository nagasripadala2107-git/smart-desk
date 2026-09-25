package com.smartdesk.dto.auth;

import com.smartdesk.entity.enums.UserRole;
import java.time.OffsetDateTime;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        UserRole role,
        boolean isActive,
        String firstName,
        String lastName,
        String customerCode,
        String employeeCode,
        OffsetDateTime createdAt
) {}
