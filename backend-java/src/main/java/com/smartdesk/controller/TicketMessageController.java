package com.smartdesk.controller;

import com.smartdesk.dto.common.ApiResponse;
import com.smartdesk.dto.message.CreateMessageRequest;
import com.smartdesk.dto.message.MessageResponse;
import com.smartdesk.entity.User;
import com.smartdesk.security.UserPrincipal;
import com.smartdesk.service.TicketMessageService;
import com.smartdesk.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tickets/{id}/messages")
public class TicketMessageController {

    private final TicketMessageService ticketMessageService;
    private final UserService userService;

    public TicketMessageController(TicketMessageService ticketMessageService, UserService userService) {
        this.ticketMessageService = ticketMessageService;
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<MessageResponse>>> getMessages(
            @PathVariable("id") UUID ticketId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        User user = userService.getUserEntity(principal.getId());
        List<MessageResponse> messages = ticketMessageService.getTicketMessages(ticketId, user);
        return ResponseEntity.ok(ApiResponse.ok(messages));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<MessageResponse>> createMessage(
            @PathVariable("id") UUID ticketId,
            @Valid @RequestBody CreateMessageRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        User user = userService.getUserEntity(principal.getId());
        MessageResponse message = ticketMessageService.createMessage(ticketId, request, user);
        return new ResponseEntity<>(ApiResponse.ok("Message sent successfully", message), HttpStatus.CREATED);
    }
}
