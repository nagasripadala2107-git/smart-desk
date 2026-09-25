package com.smartdesk.client.ai;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ClassificationResponse(
        String category,
        Double confidence,
        @JsonProperty("model_version") String modelVersion
) {}
