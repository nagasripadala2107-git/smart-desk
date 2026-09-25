package com.smartdesk.dto.analytics;

import com.smartdesk.entity.enums.TicketStatus;

public record StatusStatsResponse(
        TicketStatus status,
        long ticketCount,
        double percentage
) {}
