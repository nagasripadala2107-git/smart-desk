package com.smartdesk.dto.analytics;

public record ResolutionTimeStatsResponse(
        Double avgResolutionMinutes,
        Double minResolutionMinutes,
        Double maxResolutionMinutes,
        Double medianResolutionMinutes,
        long resolvedCount
) {}
