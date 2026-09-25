package com.smartdesk.service;

import com.smartdesk.client.ai.ClassificationResponse;
import com.smartdesk.client.ai.SentimentRequest;
import com.smartdesk.client.ai.SentimentResponse;
import com.smartdesk.client.ai.TicketSentimentClient;
import com.smartdesk.dto.message.CreateMessageRequest;
import com.smartdesk.dto.message.MessageResponse;
import com.smartdesk.dto.ticket.CreateTicketRequest;
import com.smartdesk.dto.ticket.TicketDetailResponse;
import com.smartdesk.dto.ticket.TicketResponse;
import com.smartdesk.entity.*;
import com.smartdesk.entity.enums.*;
import com.smartdesk.repository.*;
import com.smartdesk.routing.TicketRoutingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.ResourceAccessException;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketSentimentFlowTest {

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

    @Mock
    private TicketSentimentClient sentimentClient;
    @Mock
    private TicketSentimentAnalysisRepository sentimentRepository;

    private TicketSentimentService sentimentService;
    private TicketService ticketService;
    private TicketMessageService ticketMessageService;

    private User customerUser;
    private User agentUser;
    private Customer customer;
    private Category category;
    private Ticket ticket;

    @BeforeEach
    void setUp() {
        sentimentService = new TicketSentimentService(sentimentClient, sentimentRepository);

        ticketService = new TicketService(
                ticketRepository, customerRepository, categoryRepository, agentRepository,
                teamRepository, ticketEventRepository, ticketAssignmentRepository,
                ticketMessageRepository, ticketNumberGenerator, routingService,
                classificationService, auditLogService, sentimentService
        );

        ticketMessageService = new TicketMessageService(
                ticketMessageRepository, ticketEventRepository, ticketService,
                notificationService, sentimentService
        );

        customerUser = new User("customer@acme.com", "hash", UserRole.CUSTOMER);
        customerUser.setId(UUID.randomUUID());

        agentUser = new User("agent@smartdesk.local", "hash", UserRole.AGENT);
        agentUser.setId(UUID.randomUUID());

        customer = new Customer(customerUser, "CUST-001", "Acme Corp", "STANDARD");
        customer.setId(UUID.randomUUID());
        category = new Category("BILLING", "Billing & Payments");
        category.setId(UUID.randomUUID());

        ticket = new Ticket("SD-2026-000001", customer, category, "Payment issue", "Double charge", TicketPriority.HIGH);
        ticket.setId(UUID.randomUUID());
    }

    @Test
    @DisplayName("Should invoke sentiment analysis during ticket creation and persist sentiment record")
    void testTicketCreationInvokesSentimentAnalysis() {
        when(customerRepository.findByUserId(customerUser.getId())).thenReturn(Optional.of(customer));
        when(ticketNumberGenerator.generateNextTicketNumber()).thenReturn("SD-2026-000001");
        when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));
        when(classificationService.classifyTicket(any(), any())).thenReturn(Optional.empty());
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(i -> {
            Ticket t = i.getArgument(0);
            t.setId(UUID.randomUUID());
            return t;
        });

        SentimentResponse mockSentiment = new SentimentResponse("NEGATIVE", 0.8520, "FRUSTRATED", "ticket-sentiment-v1");
        when(sentimentClient.analyze(any(SentimentRequest.class))).thenReturn(mockSentiment);
        when(sentimentRepository.save(any(TicketSentimentAnalysis.class))).thenAnswer(i -> i.getArgument(0));

        CreateTicketRequest request = new CreateTicketRequest(
                "Charged twice on invoice",
                "I have been waiting for 3 days and nobody responded to my double charge.",
                TicketPriority.HIGH,
                category.getId()
        );

        TicketResponse response = ticketService.createTicket(request, customerUser);

        assertNotNull(response);
        // Verify sentiment analysis was performed and saved
        ArgumentCaptor<TicketSentimentAnalysis> captor = ArgumentCaptor.forClass(TicketSentimentAnalysis.class);
        verify(sentimentRepository, times(1)).save(captor.capture());

        TicketSentimentAnalysis saved = captor.getValue();
        assertEquals(SentimentType.NEGATIVE, saved.getSentiment());
        assertEquals(CustomerTone.FRUSTRATED, saved.getTone());
        assertEquals(new BigDecimal("0.8520"), saved.getConfidence());
        assertEquals("ticket-sentiment-v1", saved.getModelVersion());
        assertNotNull(saved.getAnalyzedTextHash());
    }

    @Test
    @DisplayName("Should analyze customer reply message and enforce message-ticket relationship")
    void testCustomerReplyMessageAnalyzesSentimentWithIntegrity() {
        when(customerRepository.findByUserId(customerUser.getId())).thenReturn(Optional.of(customer));
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        when(ticketMessageRepository.save(any(TicketMessage.class))).thenAnswer(i -> {
            TicketMessage m = i.getArgument(0);
            m.setId(UUID.randomUUID());
            return m;
        });

        SentimentResponse mockSentiment = new SentimentResponse("NEGATIVE", 0.9100, "ANGRY", "ticket-sentiment-v1");
        when(sentimentClient.analyze(any(SentimentRequest.class))).thenReturn(mockSentiment);
        when(sentimentRepository.save(any(TicketSentimentAnalysis.class))).thenAnswer(i -> i.getArgument(0));

        CreateMessageRequest request = new CreateMessageRequest(
                "This is unacceptable! I demand an immediate refund now.",
                false
        );

        MessageResponse response = ticketMessageService.createMessage(ticket.getId(), request, customerUser);

        assertNotNull(response);
        ArgumentCaptor<TicketSentimentAnalysis> captor = ArgumentCaptor.forClass(TicketSentimentAnalysis.class);
        verify(sentimentRepository, times(1)).save(captor.capture());

        TicketSentimentAnalysis saved = captor.getValue();
        assertEquals(ticket, saved.getTicket());
        assertNotNull(saved.getMessage());
        assertEquals(saved.getTicket().getId(), saved.getMessage().getTicket().getId());
        assertEquals(SentimentType.NEGATIVE, saved.getSentiment());
        assertEquals(CustomerTone.ANGRY, saved.getTone());
    }

    @Test
    @DisplayName("Should NOT analyze internal staff notes as customer sentiment")
    void testStaffInternalNoteDoesNotTriggerSentimentAnalysis() {
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        when(ticketMessageRepository.save(any(TicketMessage.class))).thenAnswer(i -> {
            TicketMessage m = i.getArgument(0);
            m.setId(UUID.randomUUID());
            return m;
        });

        CreateMessageRequest internalRequest = new CreateMessageRequest(
                "Internal note: customer seems frustrated, check stripe logs.",
                true
        );

        MessageResponse response = ticketMessageService.createMessage(ticket.getId(), internalRequest, agentUser);

        assertNotNull(response);
        verifyNoInteractions(sentimentClient);
        verifyNoInteractions(sentimentRepository);
    }

    @Test
    @DisplayName("Customer isolation: Customers receive null sentiment fields while agents receive populated sentiment")
    void testCustomerIsolationForSentimentFields() {
        when(customerRepository.findByUserId(customerUser.getId())).thenReturn(Optional.of(customer));
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        when(ticketMessageRepository.findByTicketIdAndIsInternalFalseOrderByCreatedAtAsc(ticket.getId()))
                .thenReturn(Collections.emptyList());
        when(ticketMessageRepository.findByTicketIdOrderByCreatedAtAsc(ticket.getId()))
                .thenReturn(Collections.emptyList());
        when(ticketEventRepository.findByTicketIdOrderByCreatedAtAsc(ticket.getId()))
                .thenReturn(Collections.emptyList());

        TicketSentimentAnalysis analysis = new TicketSentimentAnalysis(
                ticket, null, SentimentType.NEGATIVE, new BigDecimal("0.8900"),
                CustomerTone.FRUSTRATED, "ticket-sentiment-v1", "hash123"
        );
        when(sentimentRepository.findFirstByTicketIdOrderByCreatedAtDesc(ticket.getId()))
                .thenReturn(Optional.of(analysis));

        // 1. Customer queries ticket details -> sentiment fields MUST be null
        TicketDetailResponse customerView = ticketService.getTicketDetails(ticket.getId(), customerUser);
        assertNull(customerView.sentiment(), "Customer must NOT see sentiment");
        assertNull(customerView.sentimentConfidence(), "Customer must NOT see sentiment confidence");
        assertNull(customerView.tone(), "Customer must NOT see tone");
        assertNull(customerView.sentimentModelVersion(), "Customer must NOT see model version");

        // 2. Agent queries ticket details -> sentiment fields MUST be populated
        TicketDetailResponse agentView = ticketService.getTicketDetails(ticket.getId(), agentUser);
        assertEquals("NEGATIVE", agentView.sentiment());
        assertEquals(new BigDecimal("0.8900"), agentView.sentimentConfidence());
        assertEquals("FRUSTRATED", agentView.tone());
        assertEquals("ticket-sentiment-v1", agentView.sentimentModelVersion());
    }

    @Test
    @DisplayName("Graceful degradation: Ticket and message creation succeed when AI microservice is offline")
    void testGracefulDegradationOnAiOutage() {
        when(customerRepository.findByUserId(customerUser.getId())).thenReturn(Optional.of(customer));
        when(ticketNumberGenerator.generateNextTicketNumber()).thenReturn("SD-2026-000002");
        when(classificationService.classifyTicket(any(), any())).thenReturn(Optional.empty());
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(i -> {
            Ticket t = i.getArgument(0);
            t.setId(UUID.randomUUID());
            return t;
        });

        // AI client throws timeout or connection failure
        when(sentimentClient.analyze(any(SentimentRequest.class)))
                .thenThrow(new ResourceAccessException("I/O error: connection refused"));

        CreateTicketRequest request = new CreateTicketRequest(
                "Inquiry subject",
                "Description text",
                TicketPriority.LOW,
                null
        );

        // Must NOT throw exception
        TicketResponse response = assertDoesNotThrow(() -> ticketService.createTicket(request, customerUser));
        assertNotNull(response);
        verify(sentimentRepository, never()).save(any());
    }
}
