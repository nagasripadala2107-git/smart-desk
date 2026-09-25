package com.smartdesk.dto.message;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateMessageRequest(
        @NotBlank(message = "Message cannot be blank")
        @Size(max = 10000, message = "Message cannot exceed 10000 characters")
        String message,

        boolean isInternal
) {}
