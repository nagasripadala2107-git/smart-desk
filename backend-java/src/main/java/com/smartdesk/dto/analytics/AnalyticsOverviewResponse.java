package com.smartdesk.dto.analytics;

public record AnalyticsOverviewResponse(
        long totalTickets,
        long openTickets,
        long inProgressTickets,
        long pendingTickets,
        long escalatedTickets,
        long resolvedTickets,
        long closedTickets,
        Double avgResolutionMinutes
) {
    public AnalyticsOverviewResponse(
            long totalTickets,
            long openTickets,
            long inProgressTickets,
            long resolvedTickets,
            long escalatedTickets,
            long closedTickets
    ) {
        this(totalTickets, openTickets, inProgressTickets, 0L, escalatedTickets, resolvedTickets, closedTickets, null);
    }
}
