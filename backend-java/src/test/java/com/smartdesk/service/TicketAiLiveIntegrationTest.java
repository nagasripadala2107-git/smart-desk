package com.smartdesk.service;

import com.smartdesk.client.ai.TicketClassificationClient;
import com.smartdesk.dto.ticket.CreateTicketRequest;
import com.smartdesk.dto.ticket.TicketResponse;
import com.smartdesk.entity.Category;
import com.smartdesk.entity.Customer;
import com.smartdesk.entity.Team;
import com.smartdesk.entity.Ticket;
import com.smartdesk.entity.User;
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
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketAiLiveIntegrationTest {

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
    private AuditLogService auditLogService;

    private User customerUser;
    private Customer customer;

    @BeforeEach
    void setUp() {
        customerUser = new User("cust-integration@example.com", "hash", UserRole.CUSTOMER);
        customerUser.setId(UUID.randomUUID());

        customer = new Customer(customerUser, "CUST-INT-001", "Acme Live", "ENTERPRISE");
        customer.setId(UUID.randomUUID());
        customerUser.setCustomer(customer);
    }

    private boolean isAiServiceAvailable() {
        try (java.net.Socket socket = new java.net.Socket()) {
            socket.connect(new java.net.InetSocketAddress("127.0.0.1", 8000), 250);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Test
    @DisplayName("A. AI Service Online: Calls real FastAPI service, populates AI fields, routes to resolved team")
    void testLiveAiIntegration_WhenAiOnline_PopulatesAiFields() {
        org.junit.jupiter.api.Assumptions.assumeTrue(
                isAiServiceAvailable(),
                "FastAPI microservice is not currently running on port 8000 - skipping live HTTP verification"
        );

        // Real client pointing to live FastAPI on port 8000
        TicketClassificationClient liveClient = new TicketClassificationClient("http://127.0.0.1:8000", 2000);
        TicketClassificationService liveAiService = new TicketClassificationService(liveClient);

        TicketService ticketService = new TicketService(
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
                liveAiService,
                auditLogService
        );

        CreateTicketRequest request = new CreateTicketRequest(
                "Payment charged twice",
                "My card was charged two times for the same subscription.",
                TicketPriority.HIGH,
                null // No explicit category
        );

        Category billingCategory = new Category("BILLING", "Billing department category");
        billingCategory.setId(UUID.randomUUID());

        Team billingTeam = new Team("Billing Team", "Billing");
        billingTeam.setId(UUID.randomUUID());

        when(customerRepository.findByUserId(customerUser.getId())).thenReturn(Optional.of(customer));
        when(ticketNumberGenerator.generateNextTicketNumber()).thenReturn("SD-2026-LIVE001");
        when(categoryRepository.findByName("BILLING")).thenReturn(Optional.of(billingCategory));
        when(routingService.resolveTargetTeam(billingCategory, TicketPriority.HIGH)).thenReturn(Optional.of(billingTeam));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> {
            Ticket t = inv.getArgument(0);
            t.setId(UUID.randomUUID());
            return t;
        });

        TicketResponse response = ticketService.createTicket(request, customerUser);

        assertNotNull(response);
        assertEquals("BILLING", response.aiCategory());
        assertNotNull(response.aiConfidence());
        assertTrue(response.aiConfidence().doubleValue() > 0.0);
        assertEquals("ticket-classifier-v1", response.aiModelVersion());
        assertEquals("BILLING", response.categoryName());
        assertEquals("Billing Team", response.assignedTeamName());
        assertEquals(TicketStatus.OPEN, response.status());
    }

    @Test
    @DisplayName("B. AI Service Offline: Fast fallback, ticket creates successfully, AI fields null")
    void testLiveAiIntegration_WhenAiOffline_GracefulDegradationAndTicketCreated() {
        // Client pointing to an unavailable port (offline) with 500ms timeout
        TicketClassificationClient offlineClient = new TicketClassificationClient("http://127.0.0.1:59999", 500);
        TicketClassificationService fallbackAiService = new TicketClassificationService(offlineClient);

        TicketService ticketService = new TicketService(
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
                fallbackAiService,
                auditLogService
        );

        CreateTicketRequest request = new CreateTicketRequest(
                "Payment charged twice",
                "My card was charged two times for the same subscription.",
                TicketPriority.HIGH,
                null // No explicit category
        );

        when(customerRepository.findByUserId(customerUser.getId())).thenReturn(Optional.of(customer));
        when(ticketNumberGenerator.generateNextTicketNumber()).thenReturn("SD-2026-FALLBACK001");
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> {
            Ticket t = inv.getArgument(0);
            t.setId(UUID.randomUUID());
            return t;
        });

        TicketResponse response = ticketService.createTicket(request, customerUser);

        // Verification: Ticket MUST still create successfully
        assertNotNull(response);
        assertEquals("SD-2026-FALLBACK001", response.ticketNumber());
        assertEquals(TicketStatus.OPEN, response.status());
        assertNull(response.aiCategory());
        assertNull(response.aiConfidence());
        assertNull(response.aiModelVersion());
        assertNull(response.categoryId());
        assertNull(response.assignedTeamId());
    }

    @Test
    @DisplayName("C. Live AI Sentiment Online: Queries live FastAPI service dynamically and reports actual ML output")
    void testLiveAiSentimentIntegration_WhenAiOnline_DynamicModelOutput() {
        org.junit.jupiter.api.Assumptions.assumeTrue(
                isAiServiceAvailable(),
                "FastAPI microservice is not currently running on port 8000 - skipping live HTTP verification"
        );

        com.smartdesk.client.ai.TicketSentimentClient liveSentimentClient =
                new com.smartdesk.client.ai.TicketSentimentClient("http://127.0.0.1:8000", 2000);

        String testSentence = "Our payment was processed twice and nobody has resolved this issue for days.";
        com.smartdesk.client.ai.SentimentResponse response = liveSentimentClient.analyze(
                new com.smartdesk.client.ai.SentimentRequest(testSentence)
        );

        assertNotNull(response);
        assertNotNull(response.sentiment());
        assertNotNull(response.confidence());
        assertNotNull(response.tone());
        assertNotNull(response.modelVersion());

        System.out.println("=== LIVE AI SENTIMENT MODEL OUTPUT ===");
        System.out.println("Text: " + testSentence);
        System.out.println("Actual Sentiment: " + response.sentiment());
        System.out.println("Actual Confidence: " + response.confidence());
        System.out.println("Actual Tone: " + response.tone());
        System.out.println("Model Version: " + response.modelVersion());
        System.out.println("=======================================");

        assertDoesNotThrow(() -> com.smartdesk.entity.enums.SentimentType.valueOf(response.sentiment().toUpperCase()));
        assertTrue(response.confidence() >= 0.0 && response.confidence() <= 1.0);
        assertDoesNotThrow(() -> com.smartdesk.entity.enums.CustomerTone.valueOf(response.tone().toUpperCase()));
    }
}
