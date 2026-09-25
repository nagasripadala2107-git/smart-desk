package com.smartdesk.controller;

import com.smartdesk.dto.common.ApiResponse;
import com.smartdesk.dto.escalation.EscalationRequest;
import com.smartdesk.dto.escalation.EscalationResponse;
import com.smartdesk.entity.User;
import com.smartdesk.security.UserPrincipal;
import com.smartdesk.service.EscalationService;
import com.smartdesk.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tickets/{id}/escalate")
@PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
public class EscalationController {

    private final EscalationService escalationService;
    private final UserService userService;

    public EscalationController(EscalationService escalationService, UserService userService) {
        this.escalationService = escalationService;
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<EscalationResponse>> escalateTicket(
            @PathVariable("id") UUID ticketId,
            @Valid @RequestBody EscalationRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        User user = userService.getUserEntity(principal.getId());
        EscalationResponse escalation = escalationService.escalateTicket(ticketId, request, user);
        return ResponseEntity.ok(ApiResponse.ok("Ticket escalated successfully", escalation));
    }
}
