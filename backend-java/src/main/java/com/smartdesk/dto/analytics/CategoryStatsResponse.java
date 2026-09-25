package com.smartdesk.dto.analytics;

import java.util.UUID;

public record CategoryStatsResponse(
        UUID categoryId,
        String categoryName,
        long ticketCount,
        double percentage
) {
    public CategoryStatsResponse(UUID categoryId, String categoryName, long ticketCount) {
        this(categoryId, categoryName, ticketCount, 0.0);
    }
}
