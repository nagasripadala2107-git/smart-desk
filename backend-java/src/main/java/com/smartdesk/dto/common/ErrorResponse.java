package com.smartdesk.dto.common;

import java.time.OffsetDateTime;
import java.util.Map;

public record ErrorResponse(
        OffsetDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> validationErrors
) {
    public ErrorResponse(int status, String error, String message, String path) {
        this(OffsetDateTime.now(), status, error, message, path, null);
    }

    public ErrorResponse(int status, String error, String message, String path, Map<String, String> validationErrors) {
        this(OffsetDateTime.now(), status, error, message, path, validationErrors);
    }
}
