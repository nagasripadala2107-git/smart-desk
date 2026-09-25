package com.smartdesk.client.ai;

import java.util.List;

public record DuplicateDetectionResponse(
        List<DuplicateMatchItemDto> matches,
        String modelVersion
) {}
