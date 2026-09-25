package com.smartdesk.dto.analytics;

public record TicketVolumeResponse(
        String date,
        long count
) {}
