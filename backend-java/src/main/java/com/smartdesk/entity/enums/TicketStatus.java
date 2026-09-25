package com.smartdesk.entity.enums;

import java.util.EnumSet;
import java.util.Set;

public enum TicketStatus {
    OPEN,
    IN_PROGRESS,
    PENDING_CUSTOMER,
    PENDING_INTERNAL,
    ESCALATED,
    RESOLVED,
    CLOSED;

    public boolean canTransitionTo(TicketStatus target) {
        if (this == target) {
            return true;
        }
        return switch (this) {
            case OPEN -> target == IN_PROGRESS || target == CLOSED;
            case IN_PROGRESS -> target == PENDING_CUSTOMER || target == PENDING_INTERNAL
                             || target == ESCALATED || target == RESOLVED || target == CLOSED;
            case PENDING_CUSTOMER, PENDING_INTERNAL -> target == IN_PROGRESS || target == RESOLVED || target == CLOSED;
            case ESCALATED -> target == IN_PROGRESS || target == RESOLVED || target == CLOSED;
            case RESOLVED -> target == CLOSED || target == IN_PROGRESS;
            case CLOSED -> target == IN_PROGRESS; // Re-open
        };
    }
}
