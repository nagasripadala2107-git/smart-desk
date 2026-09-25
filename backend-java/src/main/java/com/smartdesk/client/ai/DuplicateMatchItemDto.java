package com.smartdesk.client.ai;

public record DuplicateMatchItemDto(
        String ticketId,
        Double similarity
) {}
