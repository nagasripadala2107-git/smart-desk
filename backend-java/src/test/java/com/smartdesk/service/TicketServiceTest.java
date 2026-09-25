package com.smartdesk.service;

import com.smartdesk.client.ai.ClassificationResponse;
import com.smartdesk.dto.ticket.CreateTicketRequest;
import com.smartdesk.dto.ticket.TicketResponse;
import com.smartdesk.dto.ticket.UpdateTicketRequest;
import com.smartdesk.entity.*;
import com.smartdesk.entity.enums.TicketPriority;
import com.smartdesk.entity.enums.TicketStatus;
import com.smartdesk.entity.enums.UserRole;
import com.smartdesk.exception.BadRequestException;
import com.smartdesk.repository.*;
import com.smartdesk.routing.TicketRoutingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Year;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

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

    private TicketService ticketService;

    private User customerUser;
    private Customer customer;

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

        customerUser = new User("cust@example.com", "hash", UserRole.CUSTOMER);
        customerUser.setId(UUID.randomUUID());

        customer = new Customer(customerUser, "CUST-001", "Acme", "STANDARD");
        customer.setId(UUID.randomUUID());
        customerUser.setCustomer(customer);
    }

    @Test
    void testCreateTicket_Success() {
        CreateTicketRequest request = new CreateTicketRequest(
                "Billing issue",
                "Double charged",
                TicketPriority.HIGH,
                null
        );

        String expectedNumber = "SD-" + Year.now().getValue() + "-000001";
        when(customerRepository.findByUserId(customerUser.getId())).thenReturn(Optional.of(customer));
        when(ticketNumberGenerator.generateNextTicketNumber()).thenReturn(expectedNumber);
        when(classificationService.classifyTicket(any(), any())).thenReturn(Optional.empty());
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> {
            Ticket t = inv.getArgument(0);
            t.setId(UUID.randomUUID());
            return t;
        });

        TicketResponse response = ticketService.createTicket(request, customerUser);

        assertNotNull(response);
        assertEquals(expectedNumber, response.ticketNumber());
        assertEquals("Billing issue", response.subject());
        assertEquals(TicketPriority.HIGH, response.priority());
        assertEquals(TicketStatus.OPEN, response.status());

        verify(ticketEventRepository, atLeastOnce()).save(any());
    }

    @Test
    void testCreateTicket_WithAiClassificationSuccess_AutoAssignsCategoryAndRoutes() {
        CreateTicketRequest request = new CreateTicketRequest(
                "Payment charged twice",
                "My card was charged two times for the same subscription.",
                TicketPriority.HIGH,
                null // No explicit category provided
        );

        Category billingCategory = new Category("BILLING", "Billing issues");
        billingCategory.setId(UUID.randomUUID());

        Team billingTeam = new Team("Billing", "Billing department");
        billingTeam.setId(UUID.randomUUID());

        ClassificationResponse aiResponse = new ClassificationResponse(
                "BILLING",
                0.9821,
                "ticket-classifier-v1"
        );

        when(customerRepository.findByUserId(customerUser.getId())).thenReturn(Optional.of(customer));
        when(ticketNumberGenerator.generateNextTicketNumber()).thenReturn("SD-2026-000002");
        when(classificationService.classifyTicket(request.subject(), request.description()))
                .thenReturn(Optional.of(aiResponse));
        when(categoryRepository.findByName("BILLING")).thenReturn(Optional.of(billingCategory));
        when(routingService.resolveTargetTeam(billingCategory, TicketPriority.HIGH))
                .thenReturn(Optional.of(billingTeam));

        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> {
            Ticket t = inv.getArgument(0);
            t.setId(UUID.randomUUID());
            return t;
        });

        TicketResponse response = ticketService.createTicket(request, customerUser);

        assertNotNull(response);
        assertEquals("BILLING", response.aiCategory());
        assertNotNull(response.aiConfidence());
        assertEquals(new BigDecimal("0.9821"), response.aiConfidence());
        assertEquals("ticket-classifier-v1", response.aiModelVersion());
        assertEquals("BILLING", response.categoryName());
        assertEquals("Billing", response.assignedTeamName());

        verify(ticketAssignmentRepository, times(1)).save(any(TicketAssignment.class));
    }

    @Test
    void testCreateTicket_WithExplicitCategory_PreservesExplicitCategory() {
        UUID explicitCategoryId = UUID.randomUUID();
        Category technicalCategory = new Category("TECHNICAL", "Technical issues");
        technicalCategory.setId(explicitCategoryId);

        Team techTeam = new Team("Technical Support", "Tech dept");
        techTeam.setId(UUID.randomUUID());

        CreateTicketRequest request = new CreateTicketRequest(
                "Database connection timeout",
                "Can't connect to postgres server",
                TicketPriority.URGENT,
                explicitCategoryId // Explicit category provided
        );

        ClassificationResponse aiResponse = new ClassificationResponse(
                "TECHNICAL",
                0.9410,
                "ticket-classifier-v1"
        );

        when(customerRepository.findByUserId(customerUser.getId())).thenReturn(Optional.of(customer));
        when(categoryRepository.findById(explicitCategoryId)).thenReturn(Optional.of(technicalCategory));
        when(ticketNumberGenerator.generateNextTicketNumber()).thenReturn("SD-2026-000003");
        when(classificationService.classifyTicket(request.subject(), request.description()))
                .thenReturn(Optional.of(aiResponse));
        when(routingService.resolveTargetTeam(technicalCategory, TicketPriority.URGENT))
                .thenReturn(Optional.of(techTeam));

        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> {
            Ticket t = inv.getArgument(0);
            t.setId(UUID.randomUUID());
            return t;
        });

        TicketResponse response = ticketService.createTicket(request, customerUser);

        assertNotNull(response);
        assertEquals("TECHNICAL", response.categoryName());
        assertEquals(explicitCategoryId, response.categoryId());
        assertEquals("TECHNICAL", response.aiCategory());
        assertEquals(new BigDecimal("0.9410"), response.aiConfidence());
        // Verify category was not looked up by name or overridden
        verify(categoryRepository, never()).findByName(anyString());
    }

    @Test
    void testCreateTicket_WhenAiServiceFails_TicketCreationStillSucceeds() {
        CreateTicketRequest request = new CreateTicketRequest(
                "Unknown problem",
                "Something went wrong",
                TicketPriority.LOW,
                null
        );

        when(customerRepository.findByUserId(customerUser.getId())).thenReturn(Optional.of(customer));
        when(ticketNumberGenerator.generateNextTicketNumber()).thenReturn("SD-2026-000004");
        // AI service returns empty (e.g. 500 error / unavailable)
        when(classificationService.classifyTicket(any(), any())).thenReturn(Optional.empty());

        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> {
            Ticket t = inv.getArgument(0);
            t.setId(UUID.randomUUID());
            return t;
        });

        TicketResponse response = ticketService.createTicket(request, customerUser);

        assertNotNull(response);
        assertNull(response.aiCategory());
        assertNull(response.aiConfidence());
        assertNull(response.aiModelVersion());
        assertNull(response.categoryId());
        assertEquals(TicketStatus.OPEN, response.status());
    }

    @Test
    void testStatusTransition_ValidOpenToInProgress() {
        Ticket ticket = new Ticket("SD-2026-000001", customer, null, "Bug", "Desc", TicketPriority.MEDIUM);
        ticket.setId(UUID.randomUUID());
        ticket.setStatus(TicketStatus.OPEN);

        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

        User agentUser = new User("agent@smartdesk.local", "hash", UserRole.AGENT);
        agentUser.setId(UUID.randomUUID());

        UpdateTicketRequest update = new UpdateTicketRequest(
                null, null, null, TicketStatus.IN_PROGRESS, null, null, null, "Starting work"
        );

        TicketResponse response = ticketService.updateTicket(ticket.getId(), update, agentUser);

        assertEquals(TicketStatus.IN_PROGRESS, response.status());
        verify(ticketEventRepository, times(1)).save(any());
    }

    @Test
    void testStatusTransition_InvalidOpenToResolved_ThrowsBadRequestException() {
        Ticket ticket = new Ticket("SD-2026-000001", customer, null, "Bug", "Desc", TicketPriority.MEDIUM);
        ticket.setId(UUID.randomUUID());
        ticket.setStatus(TicketStatus.OPEN);

        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));

        User agentUser = new User("agent@smartdesk.local", "hash", UserRole.AGENT);
        agentUser.setId(UUID.randomUUID());

        UpdateTicketRequest update = new UpdateTicketRequest(
                null, null, null, TicketStatus.RESOLVED, null, null, null, "Immediate resolution"
        );

        assertThrows(BadRequestException.class, () -> ticketService.updateTicket(ticket.getId(), update, agentUser));
    }
}
