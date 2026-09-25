package com.smartdesk.controller;

import com.smartdesk.dto.agent.AgentQueueResponse;
import com.smartdesk.dto.agent.AgentResponse;
import com.smartdesk.dto.common.ApiResponse;
import com.smartdesk.dto.ticket.TicketSummaryResponse;
import com.smartdesk.entity.User;
import com.smartdesk.security.UserPrincipal;
import com.smartdesk.service.AgentService;
import com.smartdesk.service.TicketService;
import com.smartdesk.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/agent")
@PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
public class AgentController {

    private final AgentService agentService;
    private final TicketService ticketService;
    private final UserService userService;

    public AgentController(AgentService agentService, TicketService ticketService, UserService userService) {
        this.agentService = agentService;
        this.ticketService = ticketService;
        this.userService = userService;
    }

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<AgentResponse>> getAgentProfile(@AuthenticationPrincipal UserPrincipal principal) {
        AgentResponse profile = agentService.getAgentDtoByUserId(principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(profile));
    }

    @GetMapping("/queue")
    public ResponseEntity<ApiResponse<AgentQueueResponse>> getAgentQueue(@AuthenticationPrincipal UserPrincipal principal) {
        AgentQueueResponse queue = agentService.getAgentQueue(principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(queue));
    }

    @GetMapping("/tickets")
    public ResponseEntity<ApiResponse<List<TicketSummaryResponse>>> getMyAssignedTickets(@AuthenticationPrincipal UserPrincipal principal) {
        User user = userService.getUserEntity(principal.getId());
        List<TicketSummaryResponse> tickets = ticketService.getAgentTickets(user);
        return ResponseEntity.ok(ApiResponse.ok(tickets));
    }
}
