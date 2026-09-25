package com.smartdesk.client.ai;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SentimentResponse(
        String sentiment,
        Double confidence,
        String tone,
        @JsonProperty("model_version") String modelVersion
) {}
