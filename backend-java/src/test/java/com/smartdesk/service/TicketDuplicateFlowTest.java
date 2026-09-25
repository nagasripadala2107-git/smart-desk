package com.smartdesk.service;

import com.smartdesk.client.ai.*;
import com.smartdesk.dto.ticket.CreateTicketRequest;
import com.smartdesk.dto.ticket.TicketDetailResponse;
import com.smartdesk.dto.ticket.TicketResponse;
import com.smartdesk.entity.*;
import com.smartdesk.entity.enums.TicketPriority;
import com.smartdesk.entity.enums.TicketStatus;
import com.smartdesk.entity.enums.UserRole;
import com.smartdesk.repository.*;
import com.smartdesk.routing.TicketRoutingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.web.client.ResourceAccessException;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketDuplicateFlowTest {

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
    private TicketSentimentService sentimentService;

    @Mock
    private TicketDuplicateClient duplicateClient;
    @Mock
    private TicketDuplicateMatchRepository duplicateMatchRepository;

    private TicketDuplicateService duplicateService;
    private TicketService ticketService;

    private User customerUser;
    private User agentUser;
    private Customer customer;
    private Category category;
    private Ticket existingTicket;

    @BeforeEach
    void setUp() {
        duplicateService = new TicketDuplicateService(duplicateClient, duplicateMatchRepository, ticketRepository);
        duplicateService.setSimilarityThreshold(0.70);

        ticketService = new TicketService(
                ticketRepository, customerRepository, categoryRepository, agentRepository,
                teamRepository, ticketEventRepository, ticketAssignmentRepository,
                ticketMessageRepository, ticketNumberGenerator, routingService,
                classificationService, auditLogService, sentimentService, duplicateService
        );

        customerUser = new User("customer@acme.com", "hash", UserRole.CUSTOMER);
        customerUser.setId(UUID.randomUUID());

        agentUser = new User("agent@smartdesk.local", "hash", UserRole.AGENT);
        agentUser.setId(UUID.randomUUID());

        customer = new Customer(customerUser, "CUST-001", "Acme Corp", "STANDARD");
        customer.setId(UUID.randomUUID());

        category = new Category("BILLING", "Billing & Payments");
        category.setId(UUID.randomUUID());

        existingTicket = new Ticket("SD-2026-000001", customer, category, "Charged twice for order", "Credit card was billed two times", TicketPriority.HIGH);
        existingTicket.setId(UUID.randomUUID());
    }

    @Test
    @DisplayName("Ticket creation runs duplicate detection and persists matches above threshold")
    void testTicketCreationRunsDuplicateDetection() {
        when(customerRepository.findByUserId(customerUser.getId())).thenReturn(Optional.of(customer));
        when(ticketNumberGenerator.generateNextTicketNumber()).thenReturn("SD-2026-000002");
        when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));
        when(classificationService.classifyTicket(any(), any())).thenReturn(Optional.empty());

        UUID newTicketId = UUID.randomUUID();
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(i -> {
            Ticket t = i.getArgument(0);
            t.setId(newTicketId);
            return t;
        });

        // Candidate search returns existingTicket
        when(ticketRepository.findCandidatesForDuplicateDetection(eq(newTicketId), any(Pageable.class)))
                .thenReturn(List.of(existingTicket));

        // Mock AI duplicate client returning 0.87 similarity
        DuplicateDetectionResponse mockAiResponse = new DuplicateDetectionResponse(
                List.of(new DuplicateMatchItemDto(existingTicket.getId().toString(), 0.87)),
                "ticket-duplicate-v1"
        );
        when(duplicateClient.analyze(any(DuplicateDetectionRequest.class))).thenReturn(mockAiResponse);
        when(duplicateMatchRepository.existsByTicketIdAndMatchedTicketId(eq(newTicketId), eq(existingTicket.getId()))).thenReturn(false);
        when(duplicateMatchRepository.save(any(TicketDuplicateMatch.class))).thenAnswer(i -> i.getArgument(0));

        CreateTicketRequest request = new CreateTicketRequest(
                "Charged two times for one order",
                "I was billed two times for order 123",
                TicketPriority.HIGH,
                category.getId()
        );

        TicketResponse response = ticketService.createTicket(request, customerUser);

        assertNotNull(response);
        assertEquals("SD-2026-000002", response.ticketNumber());
        assertEquals(TicketStatus.OPEN, response.status());

        // Verify duplicate match was saved
        verify(duplicateMatchRepository, times(1)).save(any(TicketDuplicateMatch.class));
    }

    @Test
    @DisplayName("Customer Isolation: Customer details response omits duplicateMatches, Agent details response includes them")
    void testCustomerIsolationForDuplicateMatches() {
        UUID ticketId = UUID.randomUUID();
        Ticket ticket = new Ticket("SD-2026-000003", customer, category, "Payment issue", "Double charge", TicketPriority.HIGH);
        ticket.setId(ticketId);

        when(customerRepository.findByUserId(customerUser.getId())).thenReturn(Optional.of(customer));
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(ticketMessageRepository.findByTicketIdAndIsInternalFalseOrderByCreatedAtAsc(ticketId))
                .thenReturn(Collections.emptyList());
        when(ticketMessageRepository.findByTicketIdOrderByCreatedAtAsc(ticketId))
                .thenReturn(Collections.emptyList());
        when(ticketEventRepository.findByTicketIdOrderByCreatedAtAsc(ticketId))
                .thenReturn(Collections.emptyList());

        TicketDuplicateMatch match = new TicketDuplicateMatch(
                ticket, existingTicket, new BigDecimal("0.8700"), "ticket-duplicate-v1"
        );
        when(duplicateMatchRepository.findByTicketIdOrderBySimilarityScoreDesc(ticketId))
                .thenReturn(List.of(match));

        // 1. Customer queries ticket details -> duplicateMatches MUST be null
        TicketDetailResponse customerView = ticketService.getTicketDetails(ticketId, customerUser);
        assertNull(customerView.duplicateMatches(), "Customer view must NOT contain duplicateMatches");

        // 2. Agent queries ticket details -> duplicateMatches MUST be populated
        TicketDetailResponse agentView = ticketService.getTicketDetails(ticketId, agentUser);
        assertNotNull(agentView.duplicateMatches(), "Agent view must contain duplicateMatches");
        assertEquals(1, agentView.duplicateMatches().size());
        assertEquals(existingTicket.getId(), agentView.duplicateMatches().get(0).ticketId());
        assertEquals(existingTicket.getTicketNumber(), agentView.duplicateMatches().get(0).ticketNumber());
        assertEquals(existingTicket.getSubject(), agentView.duplicateMatches().get(0).subject());
        assertEquals(new BigDecimal("0.8700"), agentView.duplicateMatches().get(0).similarityScore());
        assertEquals("ticket-duplicate-v1", agentView.duplicateMatches().get(0).modelVersion());
    }

    @Test
    @DisplayName("Graceful Degradation: Ticket creation succeeds when AI duplicate service is offline")
    void testGracefulDegradationOnDuplicateAiOutage() {
        when(customerRepository.findByUserId(customerUser.getId())).thenReturn(Optional.of(customer));
        when(ticketNumberGenerator.generateNextTicketNumber()).thenReturn("SD-2026-000004");
        when(classificationService.classifyTicket(any(), any())).thenReturn(Optional.empty());

        UUID newTicketId = UUID.randomUUID();
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(i -> {
            Ticket t = i.getArgument(0);
            t.setId(newTicketId);
            return t;
        });

        when(ticketRepository.findCandidatesForDuplicateDetection(eq(newTicketId), any(Pageable.class)))
                .thenReturn(List.of(existingTicket));

        // AI client throws connection refused
        when(duplicateClient.analyze(any(DuplicateDetectionRequest.class)))
                .thenThrow(new ResourceAccessException("Connection refused: AI service down"));

        CreateTicketRequest request = new CreateTicketRequest(
                "Billing issue",
                "Description",
                TicketPriority.MEDIUM,
                null
        );

        // Ticket creation must NOT fail
        TicketResponse response = assertDoesNotThrow(() -> ticketService.createTicket(request, customerUser));
        assertNotNull(response);
        assertEquals(TicketStatus.OPEN, response.status());
        verify(duplicateMatchRepository, never()).save(any());
    }

    @Test
    @DisplayName("Advisory Rule: System does not merge, close, or change ticket priority on duplicate detection")
    void testAdvisoryOnlyNoAutomaticActions() {
        when(customerRepository.findByUserId(customerUser.getId())).thenReturn(Optional.of(customer));
        when(ticketNumberGenerator.generateNextTicketNumber()).thenReturn("SD-2026-000005");
        when(classificationService.classifyTicket(any(), any())).thenReturn(Optional.empty());

        UUID newTicketId = UUID.randomUUID();
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(i -> {
            Ticket t = i.getArgument(0);
            t.setId(newTicketId);
            return t;
        });

        when(ticketRepository.findCandidatesForDuplicateDetection(eq(newTicketId), any(Pageable.class)))
                .thenReturn(List.of(existingTicket));

        // Exact 100% duplicate candidate
        DuplicateDetectionResponse mockAiResponse = new DuplicateDetectionResponse(
                List.of(new DuplicateMatchItemDto(existingTicket.getId().toString(), 1.00)),
                "ticket-duplicate-v1"
        );
        when(duplicateClient.analyze(any(DuplicateDetectionRequest.class))).thenReturn(mockAiResponse);
        when(duplicateMatchRepository.existsByTicketIdAndMatchedTicketId(any(), any())).thenReturn(false);
        when(duplicateMatchRepository.save(any(TicketDuplicateMatch.class))).thenAnswer(i -> i.getArgument(0));

        CreateTicketRequest request = new CreateTicketRequest(
                "Exact duplicate text",
                "Exact duplicate description",
                TicketPriority.LOW,
                null
        );

        TicketResponse response = ticketService.createTicket(request, customerUser);

        // Status remains OPEN, Priority remains LOW (no auto-escalation or auto-close)
        assertEquals(TicketStatus.OPEN, response.status());
        assertEquals(TicketPriority.LOW, response.priority());
    }
}
