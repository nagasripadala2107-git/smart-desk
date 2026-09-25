package com.smartdesk.service;

import com.smartdesk.dto.message.CreateMessageRequest;
import com.smartdesk.dto.message.MessageResponse;
import com.smartdesk.entity.Ticket;
import com.smartdesk.entity.TicketEvent;
import com.smartdesk.entity.TicketMessage;
import com.smartdesk.entity.User;
import com.smartdesk.entity.enums.EventType;
import com.smartdesk.entity.enums.UserRole;
import com.smartdesk.exception.ForbiddenException;
import com.smartdesk.mapper.EntityDtoMapper;
import com.smartdesk.repository.TicketEventRepository;
import com.smartdesk.repository.TicketMessageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class TicketMessageService {

    private final TicketMessageRepository ticketMessageRepository;
    private final TicketEventRepository ticketEventRepository;
    private final TicketService ticketService;
    private final NotificationService notificationService;
    private final TicketSentimentService sentimentService;

    public TicketMessageService(
            TicketMessageRepository ticketMessageRepository,
            TicketEventRepository ticketEventRepository,
            TicketService ticketService,
            NotificationService notificationService
    ) {
        this(ticketMessageRepository, ticketEventRepository, ticketService, notificationService, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public TicketMessageService(
            TicketMessageRepository ticketMessageRepository,
            TicketEventRepository ticketEventRepository,
            TicketService ticketService,
            NotificationService notificationService,
            TicketSentimentService sentimentService
    ) {
        this.ticketMessageRepository = ticketMessageRepository;
        this.ticketEventRepository = ticketEventRepository;
        this.ticketService = ticketService;
        this.notificationService = notificationService;
        this.sentimentService = sentimentService;
    }

    @Transactional
    public MessageResponse createMessage(UUID ticketId, CreateMessageRequest request, User senderUser) {
        Ticket ticket = ticketService.getTicketEntity(ticketId);
        ticketService.assertCanAccessTicket(ticket, senderUser);

        boolean isInternal = request.isInternal();
        // Customers are strictly prohibited from creating internal notes
        if (senderUser.getRole() == UserRole.CUSTOMER && request.isInternal()) {
            throw new ForbiddenException("Customers cannot create internal notes");
        }

        TicketMessage message = new TicketMessage(ticket, senderUser, request.message(), isInternal);
        message = ticketMessageRepository.save(message);

        // Analyze sentiment for customer public messages (Phase 8.2)
        if (senderUser.getRole() == UserRole.CUSTOMER && !isInternal && sentimentService != null) {
            sentimentService.analyzeAndPersistMessageSentiment(ticket, message, request.message());
        }

        // Record MESSAGE_ADDED event in ticket timeline
        TicketEvent event = new TicketEvent(
                ticket,
                senderUser,
                EventType.MESSAGE_ADDED,
                null,
                isInternal ? "INTERNAL_NOTE" : "PUBLIC_MESSAGE",
                "{\"messageId\": \"" + message.getId() + "\"}"
        );
        ticketEventRepository.save(event);

        // Send notifications based on message sender
        if (senderUser.getRole() == UserRole.CUSTOMER) {
            // Customer replied -> notify assigned agent if present
            if (ticket.getAssignedAgent() != null && ticket.getAssignedAgent().getUser() != null) {
                notificationService.createNotification(
                        ticket.getAssignedAgent().getUser(),
                        ticket,
                        "CUSTOMER_REPLY",
                        "New reply on ticket " + ticket.getTicketNumber(),
                        request.message()
                );
            }
        } else if (!isInternal && ticket.getCustomer() != null && ticket.getCustomer().getUser() != null) {
            // Staff replied with public message -> notify customer
            notificationService.createNotification(
                    ticket.getCustomer().getUser(),
                    ticket,
                    "AGENT_REPLY",
                    "New update on your ticket " + ticket.getTicketNumber(),
                    request.message()
            );
        }

        return EntityDtoMapper.toMessageResponse(message);
    }

    @Transactional(readOnly = true)
    public List<MessageResponse> getTicketMessages(UUID ticketId, User currentUser) {
        Ticket ticket = ticketService.getTicketEntity(ticketId);
        ticketService.assertCanAccessTicket(ticket, currentUser);

        boolean isStaff = currentUser.getRole() == UserRole.AGENT || currentUser.getRole() == UserRole.ADMIN;
        List<TicketMessage> messages = isStaff
                ? ticketMessageRepository.findByTicketIdOrderByCreatedAtAsc(ticketId)
                : ticketMessageRepository.findByTicketIdAndIsInternalFalseOrderByCreatedAtAsc(ticketId);

        return messages.stream()
                .map(EntityDtoMapper::toMessageResponse)
                .toList();
    }
}
