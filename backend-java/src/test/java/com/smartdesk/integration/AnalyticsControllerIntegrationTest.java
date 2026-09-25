package com.smartdesk.integration;

import com.smartdesk.entity.Category;
import com.smartdesk.entity.Team;
import com.smartdesk.entity.User;
import com.smartdesk.entity.enums.TicketPriority;
import com.smartdesk.entity.enums.TicketStatus;
import com.smartdesk.entity.enums.UserRole;
import com.smartdesk.repository.*;
import com.smartdesk.security.JwtTokenProvider;
import com.smartdesk.security.UserPrincipal;
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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
public class AnalyticsControllerIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String adminToken;
    private String agentToken;
    private String customerToken;

    @BeforeEach
    void setUp() {
        if (this.mockMvc == null) {
            this.mockMvc = MockMvcBuilders
                    .webAppContextSetup(webApplicationContext)
                    .apply(SecurityMockMvcConfigurers.springSecurity())
                    .build();
        }

        User admin = userRepository.findByEmail("analytics.admin@smartdesk.local")
                .orElseGet(() -> userRepository.save(new User("analytics.admin@smartdesk.local", passwordEncoder.encode("Pass123!"), UserRole.ADMIN)));
        adminToken = jwtTokenProvider.generateToken(UserPrincipal.create(admin));

        User agent = userRepository.findByEmail("analytics.agent@smartdesk.local")
                .orElseGet(() -> userRepository.save(new User("analytics.agent@smartdesk.local", passwordEncoder.encode("Pass123!"), UserRole.AGENT)));
        agentToken = jwtTokenProvider.generateToken(UserPrincipal.create(agent));

        User customer = userRepository.findByEmail("analytics.customer@smartdesk.local")
                .orElseGet(() -> userRepository.save(new User("analytics.customer@smartdesk.local", passwordEncoder.encode("Pass123!"), UserRole.CUSTOMER)));
        customerToken = jwtTokenProvider.generateToken(UserPrincipal.create(customer));
    }

    @Test
    @DisplayName("Security: Unauthenticated request is rejected (HTTP 403)")
    void testUnauthenticatedAccessRejected() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/overview")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Security: CUSTOMER role cannot access analytics (HTTP 403)")
    void testCustomerForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/overview")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Security & Contract: ADMIN can access all analytics endpoints")
    void testAdminAccessAllEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/overview")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalTickets").isNumber());

        mockMvc.perform(get("/api/v1/analytics/tickets-over-time?days=7")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(7));

        mockMvc.perform(get("/api/v1/analytics/tickets-by-category")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/v1/analytics/tickets-by-priority")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());

        mockMvc.perform(get("/api/v1/analytics/tickets-by-status")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());

        mockMvc.perform(get("/api/v1/analytics/team-performance")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/v1/analytics/agent-performance")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/v1/analytics/resolution-time")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/v1/analytics/escalations")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/v1/analytics/sla")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("Security: AGENT can access analytics endpoints")
    void testAgentAccessEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/overview")
                        .header("Authorization", "Bearer " + agentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
