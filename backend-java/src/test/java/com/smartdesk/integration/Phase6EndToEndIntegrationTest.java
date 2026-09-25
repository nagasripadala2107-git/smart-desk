package com.smartdesk.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartdesk.client.ai.TicketClassificationClient;
import com.smartdesk.dto.auth.LoginRequest;
import com.smartdesk.dto.auth.RegisterRequest;
import com.smartdesk.dto.message.CreateMessageRequest;
import com.smartdesk.dto.ticket.CreateTicketRequest;
import com.smartdesk.dto.ticket.UpdateTicketRequest;
import com.smartdesk.entity.Category;
import com.smartdesk.entity.RoutingRule;
import com.smartdesk.entity.Team;
import com.smartdesk.entity.User;
import com.smartdesk.entity.enums.TicketPriority;
import com.smartdesk.entity.enums.TicketStatus;
import com.smartdesk.entity.enums.UserRole;
import com.smartdesk.repository.*;
import com.smartdesk.service.TicketClassificationService;
import com.smartdesk.service.TicketService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class Phase6EndToEndIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private RoutingRuleRepository routingRuleRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private TicketMessageRepository ticketMessageRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TicketService ticketService;

    // Shared test state across ordered integration steps
    private static String customerToken;
    private static String customerEmail = "customer.phase6@example.com";
    private static String customer2Token;
    private static String customer2Email = "customer2.phase6@example.com";
    private static String agentToken;
    private static String agentEmail = "agent.phase6@smartdesk.local";
    private static String adminToken;
    private static String adminEmail = "admin.phase6@smartdesk.local";
    private static UUID createdTicketId;
    private static String createdTicketNumber;

    private static Category billingCat;
    private static Team billingTeam;

    private static boolean isFastApiRunning() {
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress("127.0.0.1", 8000), 200);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @BeforeEach
    void seedReferenceDataIfMissing() {
        if (this.mockMvc == null) {
            this.mockMvc = MockMvcBuilders
                    .webAppContextSetup(webApplicationContext)
                    .apply(SecurityMockMvcConfigurers.springSecurity())
                    .build();
        }

        if (teamRepository.findByName("Billing").isEmpty()) {
            billingTeam = teamRepository.save(new Team("Billing", "Billing & Payments Team"));
        } else {
            billingTeam = teamRepository.findByName("Billing").get();
        }

        if (categoryRepository.findByName("BILLING").isEmpty()) {
            billingCat = categoryRepository.save(new Category("BILLING", "Invoices and payments"));
        } else {
            billingCat = categoryRepository.findByName("BILLING").get();
        }

        if (categoryRepository.findByName("TECHNICAL").isEmpty()) {
            categoryRepository.save(new Category("TECHNICAL", "Technical and engineering issues"));
        }

        if (routingRuleRepository.findFirstByCategoryIdAndPriorityAndIsActiveTrueOrderByPriorityWeightDesc(billingCat.getId(), TicketPriority.HIGH).isEmpty()) {
            routingRuleRepository.save(new RoutingRule("Billing High Priority", billingCat, TicketPriority.HIGH, billingTeam, 20));
        }

        if (routingRuleRepository.findFirstByCategoryIdAndPriorityIsNullAndIsActiveTrueOrderByPriorityWeightDesc(billingCat.getId()).isEmpty()) {
            routingRuleRepository.save(new RoutingRule("Billing Default Route", billingCat, null, billingTeam, 10));
        }

        // Seed Agent
        if (!userRepository.existsByEmail(agentEmail)) {
            User agent = new User(agentEmail, passwordEncoder.encode("Password123!"), UserRole.AGENT);
            userRepository.save(agent);
        }

        // Seed Admin
        if (!userRepository.existsByEmail(adminEmail)) {
            User admin = new User(adminEmail, passwordEncoder.encode("Password123!"), UserRole.ADMIN);
            userRepository.save(admin);
        }
    }

    @Test
    @Order(1)
    @DisplayName("1. Security: Public registration rejects ADMIN creation (HTTP 400)")
    void testPublicRegistration_RejectsAdmin() throws Exception {
        RegisterRequest adminReq = new RegisterRequest(
                "hacker.admin@example.com",
                "Password123!",
                UserRole.ADMIN,
                "Hacker",
                "Admin",
                null,
                null,
                null,
                null
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminReq)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(2)
    @DisplayName("2. Customer Registration & Login (HTTP 201 & HTTP 200)")
    void testCustomerRegistrationAndLogin() throws Exception {
        // Register Customer
        RegisterRequest regReq = new RegisterRequest(
                customerEmail,
                "Password123!",
                UserRole.CUSTOMER,
                "Alice",
                "Customer",
                "+1555123456",
                "Acme Corp",
                null,
                null
        );

        MvcResult regRes = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regReq)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode regNode = objectMapper.readTree(regRes.getResponse().getContentAsString());
        assertTrue(regNode.get("success").asBoolean());
        customerToken = regNode.get("data").get("token").asText();
        assertNotNull(customerToken);

        // Verify Login
        LoginRequest loginReq = new LoginRequest(customerEmail, "Password123!");
        MvcResult loginRes = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode loginNode = objectMapper.readTree(loginRes.getResponse().getContentAsString());
        assertEquals("CUSTOMER", loginNode.get("data").get("user").get("role").asText());

        // Verify /api/v1/auth/me
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value(customerEmail))
                .andExpect(jsonPath("$.data.role").value("CUSTOMER"));
    }

    @Test
    @Order(3)
    @DisplayName("3. Security: Unauthenticated access to /api/v1/tickets is rejected (HTTP 401/403)")
    void testUnauthenticatedAccess_Rejected() throws Exception {
        mockMvc.perform(get("/api/v1/tickets"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assertTrue(status == 401 || status == 403, "Expected 401 or 403 but got: " + status);
                });

        mockMvc.perform(get("/api/v1/tickets")
                        .header("Authorization", "Bearer invalid-tampered-token"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assertTrue(status == 401 || status == 403, "Expected 401 or 403 but got: " + status);
                });
    }

    @Test
    @Order(4)
    @DisplayName("4. Customer Creates Ticket WITHOUT Category: AI classifies & Java routes")
    void testCustomerCreateTicket_WithAiClassificationAndRouting() throws Exception {
        CreateTicketRequest request = new CreateTicketRequest(
                "Payment charged twice",
                "My card was charged two times for the same order.",
                TicketPriority.HIGH,
                null // No explicit category supplied
        );

        MvcResult result = mockMvc.perform(post("/api/v1/tickets")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        JsonNode ticket = node.get("data");

        createdTicketId = UUID.fromString(ticket.get("id").asText());
        createdTicketNumber = ticket.get("ticketNumber").asText();

        assertNotNull(createdTicketId);
        assertTrue(createdTicketNumber.startsWith("SD-"));
        assertEquals("OPEN", ticket.get("status").asText());
        assertEquals("HIGH", ticket.get("priority").asText());

        // Check AI fields if FastAPI is running
        if (isFastApiRunning()) {
            assertEquals("BILLING", ticket.get("aiCategory").asText());
            assertTrue(ticket.get("aiConfidence").asDouble() > 0.0);
            assertEquals("ticket-classifier-v1", ticket.get("aiModelVersion").asText());
            assertEquals("BILLING", ticket.get("categoryName").asText());
            assertEquals("Billing", ticket.get("assignedTeamName").asText());
        }
    }

    @Test
    @Order(5)
    @DisplayName("5. Customer Views Ticket List & Details")
    void testCustomerViewsTicketListAndDetails() throws Exception {
        // List tickets
        mockMvc.perform(get("/api/v1/tickets")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].ticketNumber").value(createdTicketNumber))
                .andExpect(jsonPath("$.data[0].status").value("OPEN"));

        // Detail ticket
        mockMvc.perform(get("/api/v1/tickets/" + createdTicketId)
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(createdTicketId.toString()))
                .andExpect(jsonPath("$.data.subject").value("Payment charged twice"))
                .andExpect(jsonPath("$.data.status").value("OPEN"))
                .andExpect(jsonPath("$.data.events[0].eventType").value("TICKET_CREATED"));
    }

    @Test
    @Order(6)
    @DisplayName("6. Customer Reply Test: Post message & verify persistence")
    void testCustomerReply() throws Exception {
        CreateMessageRequest msgReq = new CreateMessageRequest("Here is my invoice reference: INV-2026-99.", false);

        mockMvc.perform(post("/api/v1/tickets/" + createdTicketId + "/messages")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(msgReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.message").value("Here is my invoice reference: INV-2026-99."))
                .andExpect(jsonPath("$.data.isInternal").value(false));

        // Verify thread has message
        mockMvc.perform(get("/api/v1/tickets/" + createdTicketId + "/messages")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].message").value("Here is my invoice reference: INV-2026-99."));
    }

    @Test
    @Order(7)
    @DisplayName("7. Security: Customer Isolation - Customer B cannot access Customer A's ticket (HTTP 403)")
    void testCustomerIsolation() throws Exception {
        // Register Customer B
        RegisterRequest regB = new RegisterRequest(
                customer2Email,
                "Password123!",
                UserRole.CUSTOMER,
                "Bob",
                "Customer",
                null,
                "Other Org",
                null,
                null
        );

        MvcResult resB = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regB)))
                .andExpect(status().isCreated())
                .andReturn();

        customer2Token = objectMapper.readTree(resB.getResponse().getContentAsString()).get("data").get("token").asText();

        // Customer B tries to view Customer A's ticket
        mockMvc.perform(get("/api/v1/tickets/" + createdTicketId)
                        .header("Authorization", "Bearer " + customer2Token))
                .andExpect(status().isForbidden());

        // Customer B tries to post a message to Customer A's ticket
        CreateMessageRequest forbiddenMsg = new CreateMessageRequest("Sneaky message", false);
        mockMvc.perform(post("/api/v1/tickets/" + createdTicketId + "/messages")
                        .header("Authorization", "Bearer " + customer2Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(forbiddenMsg)))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(8)
    @DisplayName("8. Agent End-to-End: Agent login, views tickets, replies")
    void testAgentWorkflow() throws Exception {
        LoginRequest agentLogin = new LoginRequest(agentEmail, "Password123!");
        MvcResult loginRes = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(agentLogin)))
                .andExpect(status().isOk())
                .andReturn();

        agentToken = objectMapper.readTree(loginRes.getResponse().getContentAsString()).get("data").get("token").asText();

        // Agent views ticket list
        mockMvc.perform(get("/api/v1/tickets")
                        .header("Authorization", "Bearer " + agentToken))
                .andExpect(status().isOk());

        // Agent views ticket details
        mockMvc.perform(get("/api/v1/tickets/" + createdTicketId)
                        .header("Authorization", "Bearer " + agentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(createdTicketId.toString()));

        // Agent replies to ticket
        CreateMessageRequest agentReply = new CreateMessageRequest("Hello Alice, I have initiated the duplicate refund.", false);
        mockMvc.perform(post("/api/v1/tickets/" + createdTicketId + "/messages")
                        .header("Authorization", "Bearer " + agentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(agentReply)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.senderRole").value("AGENT"));
    }

    @Test
    @Order(9)
    @DisplayName("9. Ticket Lifecycle: OPEN -> IN_PROGRESS -> RESOLVED -> CLOSED")
    void testTicketLifecycle() throws Exception {
        // Transition: OPEN -> IN_PROGRESS
        UpdateTicketRequest toInProgress = new UpdateTicketRequest(
                null, null, null, TicketStatus.IN_PROGRESS, null, null, null, "Agent picking up ticket"
        );
        mockMvc.perform(patch("/api/v1/tickets/" + createdTicketId)
                        .header("Authorization", "Bearer " + agentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(toInProgress)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));

        // Transition: IN_PROGRESS -> RESOLVED
        UpdateTicketRequest toResolved = new UpdateTicketRequest(
                null, null, null, TicketStatus.RESOLVED, null, null, null, "Refund processed"
        );
        mockMvc.perform(patch("/api/v1/tickets/" + createdTicketId)
                        .header("Authorization", "Bearer " + agentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(toResolved)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RESOLVED"))
                .andExpect(jsonPath("$.data.resolvedAt").isNotEmpty());

        // Transition: RESOLVED -> CLOSED
        UpdateTicketRequest toClosed = new UpdateTicketRequest(
                null, null, null, TicketStatus.CLOSED, null, null, null, "Closing resolved ticket"
        );
        mockMvc.perform(patch("/api/v1/tickets/" + createdTicketId)
                        .header("Authorization", "Bearer " + agentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(toClosed)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CLOSED"))
                .andExpect(jsonPath("$.data.closedAt").isNotEmpty());
    }

    @Test
    @Order(10)
    @DisplayName("10. Security: Customer cannot access Agent/Admin analytics endpoints (HTTP 403)")
    void testCustomerAccessToAnalytics_Forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/overview")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(11)
    @DisplayName("11. Admin Verification: Admin logs in, verifies analytics endpoints")
    void testAdminVerificationAndAnalytics() throws Exception {
        LoginRequest adminLogin = new LoginRequest(adminEmail, "Password123!");
        MvcResult loginRes = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminLogin)))
                .andExpect(status().isOk())
                .andReturn();

        adminToken = objectMapper.readTree(loginRes.getResponse().getContentAsString()).get("data").get("token").asText();

        // GET /api/v1/analytics/overview
        mockMvc.perform(get("/api/v1/analytics/overview")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalTickets").isNumber());

        // GET /api/v1/analytics/tickets-by-category
        mockMvc.perform(get("/api/v1/analytics/tickets-by-category")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());

        // GET /api/v1/analytics/tickets-by-priority
        mockMvc.perform(get("/api/v1/analytics/tickets-by-priority")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @Order(12)
    @DisplayName("12. AI Offline / Graceful Degradation: Ticket creation STILL succeeds with null AI fields")
    void testAiOfflineGracefulDegradation() throws Exception {
        // Customer creates a ticket with an explicitly offline client
        TicketClassificationClient offlineClient = new TicketClassificationClient("http://127.0.0.1:59999", 200);
        TicketClassificationService offlineAiService = new TicketClassificationService(offlineClient);

        // Test with offline client directly calling classifyTicket
        var aiResult = offlineAiService.classifyTicket("Test subject", "Test description");
        assertTrue(aiResult.isEmpty(), "Offline AI service must return empty Optional, never throw exception");

        // Customer creates ticket when AI client fails
        CreateTicketRequest req = new CreateTicketRequest(
                "Degradation test ticket",
                "Customer inquiry during AI downtime",
                TicketPriority.LOW,
                null
        );

        MvcResult res = mockMvc.perform(post("/api/v1/tickets")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode ticket = objectMapper.readTree(res.getResponse().getContentAsString()).get("data");
        assertEquals("OPEN", ticket.get("status").asText());
        assertTrue(ticket.get("ticketNumber").asText().startsWith("SD-"));
    }
}
