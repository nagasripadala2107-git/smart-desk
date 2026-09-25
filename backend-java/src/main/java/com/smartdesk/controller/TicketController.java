package com.smartdesk.controller;

import com.smartdesk.dto.common.ApiResponse;
import com.smartdesk.dto.ticket.*;
import com.smartdesk.entity.Ticket;
import com.smartdesk.entity.User;
import com.smartdesk.entity.enums.UserRole;
import com.smartdesk.security.UserPrincipal;
import com.smartdesk.service.TicketService;
import com.smartdesk.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tickets")
public class TicketController {

    private final TicketService ticketService;
    private final UserService userService;

    public TicketController(TicketService ticketService, UserService userService) {
        this.ticketService = ticketService;
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TicketResponse>> createTicket(
            @Valid @RequestBody CreateTicketRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        User user = userService.getUserEntity(principal.getId());
        TicketResponse ticket = ticketService.createTicket(request, user);
        return new ResponseEntity<>(ApiResponse.ok("Ticket created successfully", ticket), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<TicketSummaryResponse>>> listTickets(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        User user = userService.getUserEntity(principal.getId());
        List<TicketSummaryResponse> tickets;
        if (user.getRole() == UserRole.CUSTOMER) {
            tickets = ticketService.getCustomerTickets(user);
        } else {
            tickets = ticketService.getAllTickets();
        }
        return ResponseEntity.ok(ApiResponse.ok(tickets));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TicketDetailResponse>> getTicket(
            @PathVariable("id") UUID ticketId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        User user = userService.getUserEntity(principal.getId());
        TicketDetailResponse ticket = ticketService.getTicketDetails(ticketId, user);
        return ResponseEntity.ok(ApiResponse.ok(ticket));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<TicketResponse>> updateTicket(
            @PathVariable("id") UUID ticketId,
            @Valid @RequestBody UpdateTicketRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        User user = userService.getUserEntity(principal.getId());
        TicketResponse ticket = ticketService.updateTicket(ticketId, request, user);
        return ResponseEntity.ok(ApiResponse.ok("Ticket updated successfully", ticket));
    }

    @PostMapping("/{id}/auto-route")
    public ResponseEntity<ApiResponse<TicketResponse>> autoRouteTicket(
            @PathVariable("id") UUID ticketId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        User user = userService.getUserEntity(principal.getId());
        Ticket ticket = ticketService.getTicketEntity(ticketId);
        ticketService.assertCanAccessTicket(ticket, user);

        // Placeholder for auto-routing trigger
        // In Phase 5: Python AI classifier determines category -> auto-route evaluates team
        return ResponseEntity.ok(ApiResponse.ok("Auto-route boundary invoked", com.smartdesk.mapper.EntityDtoMapper.toTicketResponse(ticket)));
    }
}
