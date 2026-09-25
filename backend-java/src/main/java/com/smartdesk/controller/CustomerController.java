package com.smartdesk.controller;

import com.smartdesk.dto.common.ApiResponse;
import com.smartdesk.dto.customer.CustomerProfileResponse;
import com.smartdesk.dto.ticket.TicketSummaryResponse;
import com.smartdesk.entity.User;
import com.smartdesk.security.UserPrincipal;
import com.smartdesk.service.CustomerService;
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
@RequestMapping("/api/v1/customer")
@PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
public class CustomerController {

    private final CustomerService customerService;
    private final TicketService ticketService;
    private final UserService userService;

    public CustomerController(CustomerService customerService, TicketService ticketService, UserService userService) {
        this.customerService = customerService;
        this.ticketService = ticketService;
        this.userService = userService;
    }

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<CustomerProfileResponse>> getProfile(@AuthenticationPrincipal UserPrincipal principal) {
        CustomerProfileResponse profile = customerService.getCustomerProfile(principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(profile));
    }

    @GetMapping("/tickets")
    public ResponseEntity<ApiResponse<List<TicketSummaryResponse>>> getMyTickets(@AuthenticationPrincipal UserPrincipal principal) {
        User user = userService.getUserEntity(principal.getId());
        List<TicketSummaryResponse> tickets = ticketService.getCustomerTickets(user);
        return ResponseEntity.ok(ApiResponse.ok(tickets));
    }
}
