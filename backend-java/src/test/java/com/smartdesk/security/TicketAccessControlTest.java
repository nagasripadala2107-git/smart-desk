package com.smartdesk.security;

import com.smartdesk.dto.message.MessageResponse;
import com.smartdesk.entity.*;
import com.smartdesk.entity.enums.TicketPriority;
import com.smartdesk.entity.enums.UserRole;
import com.smartdesk.exception.ForbiddenException;
import com.smartdesk.repository.*;
import com.smartdesk.routing.TicketRoutingService;
import com.smartdesk.service.AuditLogService;
import com.smartdesk.service.NotificationService;
import com.smartdesk.service.TicketClassificationService;
import com.smartdesk.service.TicketMessageService;
import com.smartdesk.service.TicketNumberGenerator;
import com.smartdesk.service.TicketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketAccessControlTest {

    @Mock
    private TicketRepository ticketRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private AgentRepository agentRepository;
    @Mock
    private TeamRepository teamRepository;
    @Mock
    private TicketEventRepository ticketEventRepository;
    @Mock
    private TicketAssignmentRepository ticketAssignmentRepository;
    @Mock
    private TicketMessageRepository ticketMessageRepository;
    @Mock
    private TicketNumberGenerator ticketNumberGenerator;
    @Mock
    private TicketRoutingService routingService;
    @Mock
    private TicketClassificationService classificationService;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private NotificationService notificationService;

    private TicketService ticketService;
    private TicketMessageService ticketMessageService;

    private User customerUser1;
    private Customer customer1;

    private User customerUser2;
    private Customer customer2;

    private Ticket ticket1;

    @BeforeEach
    void setUp() {
        ticketService = new TicketService(
                ticketRepository,
                customerRepository,
                categoryRepository,
                agentRepository,
                teamRepository,
                ticketEventRepository,
                ticketAssignmentRepository,
                ticketMessageRepository,
                ticketNumberGenerator,
                routingService,
                classificationService,
                auditLogService
        );

        ticketMessageService = new TicketMessageService(
                ticketMessageRepository,
                ticketEventRepository,
                ticketService,
                notificationService
        );

        customerUser1 = new User("cust1@example.com", "hash", UserRole.CUSTOMER);
        customerUser1.setId(UUID.randomUUID());
        customer1 = new Customer(customerUser1, "CUST-001", "Acme", "STANDARD");
        customer1.setId(UUID.randomUUID());
        customerUser1.setCustomer(customer1);

        customerUser2 = new User("cust2@example.com", "hash", UserRole.CUSTOMER);
        customerUser2.setId(UUID.randomUUID());
        customer2 = new Customer(customerUser2, "CUST-002", "Globex", "STANDARD");
        customer2.setId(UUID.randomUUID());
        customerUser2.setCustomer(customer2);

        ticket1 = new Ticket("SD-2026-000001", customer1, null, "Subject", "Desc", TicketPriority.MEDIUM);
        ticket1.setId(UUID.randomUUID());
    }

    @Test
    void testCustomerCannotAccessAnotherCustomersTicket() {
        when(customerRepository.findByUserId(customerUser2.getId())).thenReturn(Optional.of(customer2));

        assertThrows(ForbiddenException.class, () ->
                ticketService.assertCanAccessTicket(ticket1, customerUser2)
        );
    }

    @Test
    void testCustomerCanAccessOwnTicket() {
        when(customerRepository.findByUserId(customerUser1.getId())).thenReturn(Optional.of(customer1));

        assertDoesNotThrow(() ->
                ticketService.assertCanAccessTicket(ticket1, customerUser1)
        );
    }

    @Test
    void testCustomerCannotSeeInternalMessages() {
        when(ticketRepository.findById(ticket1.getId())).thenReturn(Optional.of(ticket1));
        when(customerRepository.findByUserId(customerUser1.getId())).thenReturn(Optional.of(customer1));

        TicketMessage publicMsg = new TicketMessage(ticket1, customerUser1, "Public message", false);
        publicMsg.setId(UUID.randomUUID());

        // When customer requests messages, repository query with isInternal=false is called
        when(ticketMessageRepository.findByTicketIdAndIsInternalFalseOrderByCreatedAtAsc(ticket1.getId()))
                .thenReturn(List.of(publicMsg));

        List<MessageResponse> responses = ticketMessageService.getTicketMessages(ticket1.getId(), customerUser1);

        assertEquals(1, responses.size());
        assertFalse(responses.get(0).isInternal());
        verify(ticketMessageRepository, never()).findByTicketIdOrderByCreatedAtAsc(any());
        verify(ticketMessageRepository, times(1)).findByTicketIdAndIsInternalFalseOrderByCreatedAtAsc(ticket1.getId());
    }

    @Test
    void testAgentCanSeeAllMessagesIncludingInternal() {
        User agentUser = new User("agent@smartdesk.local", "hash", UserRole.AGENT);
        agentUser.setId(UUID.randomUUID());

        when(ticketRepository.findById(ticket1.getId())).thenReturn(Optional.of(ticket1));

        TicketMessage publicMsg = new TicketMessage(ticket1, customerUser1, "Public message", false);
        publicMsg.setId(UUID.randomUUID());
        TicketMessage internalMsg = new TicketMessage(ticket1, agentUser, "Private internal note", true);
        internalMsg.setId(UUID.randomUUID());

        when(ticketMessageRepository.findByTicketIdOrderByCreatedAtAsc(ticket1.getId()))
                .thenReturn(List.of(publicMsg, internalMsg));

        List<MessageResponse> responses = ticketMessageService.getTicketMessages(ticket1.getId(), agentUser);

        assertEquals(2, responses.size());
        verify(ticketMessageRepository, times(1)).findByTicketIdOrderByCreatedAtAsc(ticket1.getId());
        verify(ticketMessageRepository, never()).findByTicketIdAndIsInternalFalseOrderByCreatedAtAsc(any());
    }
}
