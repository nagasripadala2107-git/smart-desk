package com.smartdesk.dto.analytics;

import com.smartdesk.entity.enums.TicketPriority;

public record PriorityStatsResponse(
        TicketPriority priority,
        long ticketCount,
        double percentage
) {
    public PriorityStatsResponse(TicketPriority priority, long ticketCount) {
        this(priority, ticketCount, 0.0);
    }
}
