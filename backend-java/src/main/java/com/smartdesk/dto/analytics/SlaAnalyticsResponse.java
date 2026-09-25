package com.smartdesk.dto.analytics;

import com.smartdesk.entity.enums.TicketPriority;
import java.util.List;
import java.util.UUID;

public record SlaAnalyticsResponse(
        int configuredPoliciesCount,
        long ticketsPastResponseTargetCount,
        long ticketsPastResolutionTargetCount,
        List<SlaPolicyMetricDto> policies
) {
    public record SlaPolicyMetricDto(
            UUID id,
            String name,
            TicketPriority priority,
            int firstResponseMinutes,
            int resolutionMinutes,
            boolean isActive
    ) {}
}
