package com.smartdesk.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartdesk.dto.auth.LoginRequest;
import com.smartdesk.dto.auth.RegisterRequest;
import com.smartdesk.dto.ticket.CreateTicketRequest;
import com.smartdesk.dto.ticket.UpdateTicketRequest;
import com.smartdesk.entity.Customer;
import com.smartdesk.entity.Ticket;
import com.smartdesk.entity.User;
import com.smartdesk.entity.enums.TicketPriority;
import com.smartdesk.entity.enums.UserRole;
import com.smartdesk.repository.CustomerRepository;
import com.smartdesk.repository.TicketRepository;
import com.smartdesk.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
public class SecurityHardeningIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RateLimitingFilter rateLimitingFilter;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private User customerUser1;
    private Customer customer1;
    private User customerUser2;
    private Customer customer2;
    private User agentUser;
    private User adminUser;

    private String customer1Token;
    private String customer2Token;
    private String agentToken;
    private String adminToken;

    private Ticket ticket1;

    @BeforeEach
    void setUp() {
        rateLimitingFilter.reset();
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();

        // Seed isolated test users if not present
        customerUser1 = getOrCreateUser("sec_cust1@smartdesk.local", UserRole.CUSTOMER);
        customer1 = customerRepository.findByUserId(customerUser1.getId()).orElseGet(() -> {
            Customer c = new Customer(customerUser1, "CUST-SEC-1", "Security Cust 1", "STANDARD");
            return customerRepository.save(c);
        });

        customerUser2 = getOrCreateUser("sec_cust2@smartdesk.local", UserRole.CUSTOMER);
        customer2 = customerRepository.findByUserId(customerUser2.getId()).orElseGet(() -> {
            Customer c = new Customer(customerUser2, "CUST-SEC-2", "Security Cust 2", "STANDARD");
            return customerRepository.save(c);
        });

        agentUser = getOrCreateUser("sec_agent@smartdesk.local", UserRole.AGENT);
        adminUser = getOrCreateUser("sec_admin@smartdesk.local", UserRole.ADMIN);

        customer1Token = jwtTokenProvider.generateToken(UserPrincipal.create(customerUser1));
        customer2Token = jwtTokenProvider.generateToken(UserPrincipal.create(customerUser2));
        agentToken = jwtTokenProvider.generateToken(UserPrincipal.create(agentUser));
        adminToken = jwtTokenProvider.generateToken(UserPrincipal.create(adminUser));

        ticket1 = ticketRepository.findByTicketNumber("SD-SEC-000001").orElseGet(() -> {
            Ticket t = new Ticket("SD-SEC-000001", customer1, null, "Security Test Ticket", "Description", TicketPriority.MEDIUM);
            return ticketRepository.save(t);
        });
    }

    private User getOrCreateUser(String email, UserRole role) {
        return userRepository.findByEmail(email).orElseGet(() -> {
            User u = new User(email, passwordEncoder.encode("Password123!"), role);
            return userRepository.save(u);
        });
    }

    @Test
    @DisplayName("Unauthenticated request to protected API must return 401 or 403")
    void testUnauthenticatedAccessReturnsDenied() throws Exception {
        mockMvc.perform(get("/api/v1/tickets"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("Invalid JWT token must return 401 or 403")
    void testInvalidTokenReturnsDenied() throws Exception {
        mockMvc.perform(get("/api/v1/tickets")
                        .header("Authorization", "Bearer invalid.jwt.signature"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("Expired or malformed Authorization header must return 401 or 403")
    void testMalformedAuthHeaderReturnsDenied() throws Exception {
        mockMvc.perform(get("/api/v1/tickets")
                        .header("Authorization", "NotBearer Token"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("Customer accessing analytics endpoint must be rejected with 403 Forbidden")
    void testCustomerCannotAccessAnalytics() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/overview")
                        .header("Authorization", "Bearer " + customer1Token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Customer accessing agent endpoint must be rejected with 403 Forbidden")
    void testCustomerCannotAccessAgentEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/agent/queue")
                        .header("Authorization", "Bearer " + customer1Token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("IDOR Prevention: Customer cannot access another customer's ticket")
    void testCustomerCannotAccessOtherCustomerTicket() throws Exception {
        // Customer 2 attempts to access Ticket 1 (owned by Customer 1)
        mockMvc.perform(get("/api/v1/tickets/" + ticket1.getId())
                        .header("Authorization", "Bearer " + customer2Token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("IDOR Prevention: Customer cannot update assignments on tickets")
    void testCustomerCannotUpdateTicketAssignments() throws Exception {
        UpdateTicketRequest updateReq = new UpdateTicketRequest(
                null, null, null, null, null,
                UUID.randomUUID(), null, "Illegal assignment attempt"
        );

        mockMvc.perform(patch("/api/v1/tickets/" + ticket1.getId())
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Customer isolation: Customer accessing own ticket gets duplicateMatches=null")
    void testCustomerDetailResponseSuppressesDuplicateMatches() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/tickets/" + ticket1.getId())
                        .header("Authorization", "Bearer " + customer1Token))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        assertTrue(responseBody.contains("\"duplicateMatches\":null") || !responseBody.contains("\"duplicateMatches\":["));
    }

    @Test
    @DisplayName("Public registration rejects AGENT or ADMIN role elevation")
    void testPublicRegistrationRejectsElevatedRoles() throws Exception {
        RegisterRequest adminRegister = new RegisterRequest(
                "hacker_admin@example.com",
                "Password123!",
                UserRole.ADMIN,
                "Hacker",
                "Admin",
                null, null, null, null
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminRegister)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
    }

    @Test
    @DisplayName("Malformed JSON request returns 400 Bad Request instead of 500")
    void testMalformedJsonReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"test@example.com\", invalid-json}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MALFORMED_REQUEST"));
    }

    @Test
    @DisplayName("Invalid UUID format in URL path returns 400 Bad Request instead of 500")
    void testInvalidUuidReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/tickets/not-a-valid-uuid")
                        .header("Authorization", "Bearer " + agentToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_PARAMETER"));
    }

    @Test
    @DisplayName("Security response headers are properly set on HTTP responses")
    void testSecurityResponseHeadersPresent() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().exists("Referrer-Policy"))
                .andExpect(header().exists("Permissions-Policy"));
    }

    @Test
    @DisplayName("Unauthenticated /api/v1/auth/me returns 401 Unauthorized")
    void testUnauthenticatedMeEndpointReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("Rate limiting filter enforces 429 Too Many Requests upon rapid abuse")
    void testRateLimitingEnforcement() throws Exception {
        RateLimitingFilter tightLimiter = new RateLimitingFilter(3);
        MockMvc rateLimitedMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .addFilter(tightLimiter)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();

        LoginRequest loginRequest = new LoginRequest("sec_cust1@smartdesk.local", "Password123!");
        String json = objectMapper.writeValueAsString(loginRequest);

        // First 3 requests succeed
        for (int i = 0; i < 3; i++) {
            rateLimitedMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isOk());
        }

        // 4th request must be blocked with 429 TOO_MANY_REQUESTS
        rateLimitedMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error").value("TOO_MANY_REQUESTS"));
    }

    @Test
    @DisplayName("JWT Security: Expired JWT token is rejected")
    void testExpiredJwtTokenRejected() throws Exception {
        String expiredToken = jwtTokenProvider.generateExpiredToken(UserPrincipal.create(customerUser1));

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("JWT Security: Inactive/deactivated user with valid JWT is rejected")
    void testDeactivatedUserWithJwtRejected() throws Exception {
        User deactUser = getOrCreateUser("deact_user@smartdesk.local", UserRole.CUSTOMER);
        deactUser.setActive(true);
        userRepository.save(deactUser);

        String token = jwtTokenProvider.generateToken(UserPrincipal.create(deactUser));

        // Now deactivate user
        deactUser.setActive(false);
        userRepository.save(deactUser);

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());

        // Restore user state
        deactUser.setActive(true);
        userRepository.save(deactUser);
    }

    @Test
    @DisplayName("Authorization Regression: Customer cannot modify another customer's ticket details")
    void testCustomerCannotUpdateOtherCustomerTicketDetails() throws Exception {
        UpdateTicketRequest updateReq = new UpdateTicketRequest(
                "Hacked Subject", "Hacked Description", null, null, null,
                null, null, "Illegal update attempt"
        );

        mockMvc.perform(patch("/api/v1/tickets/" + ticket1.getId())
                        .header("Authorization", "Bearer " + customer2Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Authorization Regression: Customer cannot invoke escalation endpoint")
    void testCustomerCannotEscalateTicket() throws Exception {
        com.smartdesk.dto.escalation.EscalationRequest escReq = new com.smartdesk.dto.escalation.EscalationRequest(
                UUID.randomUUID(), null, 1, "Customer escalation attempt"
        );

        mockMvc.perform(post("/api/v1/tickets/" + ticket1.getId() + "/escalate")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(escReq)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Cache-Control Header: Sensitive authenticated endpoints have no-cache headers")
    void testCacheControlOnSensitiveEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + customer1Token))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-cache")))
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
                .andExpect(header().string("Pragma", "no-cache"));
    }

    @Test
    @DisplayName("Runtime SQL Injection Test: Injection payload in login query is rejected safely without SQL errors")
    void testSqlInjectionInLoginHandledSafely() throws Exception {
        // Syntactically valid email with SQL injection payload reaches JPA prepared statement
        LoginRequest sqlInjectReq = new LoginRequest("attacker'OR'1'='1@smartdesk.local", "password");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sqlInjectReq)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));

        // Raw SQL injection string that is not a valid email is rejected at validation layer
        LoginRequest rawSqlInjectReq = new LoginRequest("' OR '1'='1' --", "password");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rawSqlInjectReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    @Test
    @DisplayName("Runtime XSS Test: Stored HTML/script tags in ticket payload are safely treated as literal text")
    void testXssPayloadHandledSafely() throws Exception {
        String xssSubject = "XSS Test <script>alert('xss')</script>";
        String xssDescription = "Description with <img src='x' onerror='alert(1)'> payload.";
        CreateTicketRequest xssReq = new CreateTicketRequest(
                xssSubject, xssDescription, TicketPriority.LOW, null
        );

        MvcResult result = mockMvc.perform(post("/api/v1/tickets")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(xssReq)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Content-Type", org.hamcrest.Matchers.containsString("application/json")))
                .andExpect(jsonPath("$.data.subject").value(xssSubject))
                .andReturn();

        assertTrue(result.getResponse().getContentAsString().contains("<script>alert('xss')</script>"));
    }
}
