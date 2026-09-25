package com.smartdesk.client.ai;

import java.util.List;

public record DuplicateDetectionRequest(
        String text,
        List<DuplicateCandidateDto> candidates
) {}
