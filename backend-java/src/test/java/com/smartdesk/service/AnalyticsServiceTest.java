package com.smartdesk.service;

import com.smartdesk.dto.analytics.*;
import com.smartdesk.dto.analytics.EscalationStatsResponse.EscalationSummaryDto;
import com.smartdesk.entity.*;
import com.smartdesk.entity.enums.AgentAvailability;
import com.smartdesk.entity.enums.EscalationStatus;
import com.smartdesk.entity.enums.TicketPriority;
import com.smartdesk.entity.enums.TicketStatus;
import com.smartdesk.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private TicketRepository ticketRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private TeamRepository teamRepository;
    @Mock
    private AgentRepository agentRepository;
    @Mock
    private EscalationRepository escalationRepository;
    @Mock
    private SlaPolicyRepository slaPolicyRepository;

    @InjectMocks
    private AnalyticsService analyticsService;

    private OffsetDateTime now;

    @BeforeEach
    void setUp() {
        now = OffsetDateTime.now(ZoneOffset.UTC);
    }

    @Test
    void testGetOverview() {
        when(ticketRepository.count()).thenReturn(10L);
        when(ticketRepository.countByStatus(TicketStatus.OPEN)).thenReturn(3L);
        when(ticketRepository.countByStatus(TicketStatus.IN_PROGRESS)).thenReturn(2L);
        when(ticketRepository.countByStatus(TicketStatus.PENDING_CUSTOMER)).thenReturn(1L);
        when(ticketRepository.countByStatus(TicketStatus.PENDING_INTERNAL)).thenReturn(0L);
        when(ticketRepository.countByStatus(TicketStatus.ESCALATED)).thenReturn(1L);
        when(ticketRepository.countByStatus(TicketStatus.RESOLVED)).thenReturn(2L);
        when(ticketRepository.countByStatus(TicketStatus.CLOSED)).thenReturn(1L);

        List<Object[]> resRows = new ArrayList<>();
        resRows.add(new Object[]{now.minusHours(4), now.minusHours(2)}); // 120 minutes
        when(ticketRepository.findResolutionTimestamps()).thenReturn(resRows);

        AnalyticsOverviewResponse res = analyticsService.getOverview();

        assertEquals(10L, res.totalTickets());
        assertEquals(3L, res.openTickets());
        assertEquals(2L, res.inProgressTickets());
        assertEquals(1L, res.pendingTickets());
        assertEquals(1L, res.escalatedTickets());
        assertEquals(2L, res.resolvedTickets());
        assertEquals(1L, res.closedTickets());
        assertNotNull(res.avgResolutionMinutes());
        assertEquals(120.0, res.avgResolutionMinutes());
    }

    @Test
    void testGetTicketsOverTime() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        List<Object[]> rows = new ArrayList<>();
        rows.add(new Object[]{today.minusDays(2), 5L});
        rows.add(new Object[]{today, 3L});
        when(ticketRepository.countTicketsByDaySince(any())).thenReturn(rows);

        List<TicketVolumeResponse> result = analyticsService.getTicketsOverTime(7);

        assertEquals(7, result.size());
        TicketVolumeResponse last = result.get(result.size() - 1);
        assertEquals(today.toString(), last.date());
        assertEquals(3L, last.count());
    }

    @Test
    void testGetTicketsByCategory() {
        UUID catId = UUID.randomUUID();
        Category cat = new Category("Technical Support", "Tech queries");
        cat.setId(catId);

        when(ticketRepository.count()).thenReturn(10L);
        when(categoryRepository.findAll()).thenReturn(List.of(cat));
        List<Object[]> catRows = new ArrayList<>();
        catRows.add(new Object[]{catId, "Technical Support", 4L});
        when(ticketRepository.countGroupByAssignedCategory()).thenReturn(catRows);

        List<CategoryStatsResponse> result = analyticsService.getTicketsByCategory();

        assertEquals(1, result.size());
        assertEquals("Technical Support", result.get(0).categoryName());
        assertEquals(4L, result.get(0).ticketCount());
        assertEquals(40.0, result.get(0).percentage());
    }

    @Test
    void testGetTicketsByPriority() {
        when(ticketRepository.count()).thenReturn(10L);
        List<Object[]> priRows = new ArrayList<>();
        priRows.add(new Object[]{TicketPriority.HIGH, 7L});
        priRows.add(new Object[]{TicketPriority.URGENT, 3L});
        when(ticketRepository.countGroupByPriority()).thenReturn(priRows);

        List<PriorityStatsResponse> result = analyticsService.getTicketsByPriority();

        assertEquals(4, result.size());
        PriorityStatsResponse high = result.stream().filter(p -> p.priority() == TicketPriority.HIGH).findFirst().orElseThrow();
        assertEquals(7L, high.ticketCount());
        assertEquals(70.0, high.percentage());
    }

    @Test
    void testGetTicketsByStatus() {
        when(ticketRepository.count()).thenReturn(5L);
        List<Object[]> statusRows = new ArrayList<>();
        statusRows.add(new Object[]{TicketStatus.OPEN, 3L});
        statusRows.add(new Object[]{TicketStatus.CLOSED, 2L});
        when(ticketRepository.countGroupByStatus()).thenReturn(statusRows);

        List<StatusStatsResponse> result = analyticsService.getTicketsByStatus();

        assertEquals(TicketStatus.values().length, result.size());
        StatusStatsResponse open = result.stream().filter(s -> s.status() == TicketStatus.OPEN).findFirst().orElseThrow();
        assertEquals(3L, open.ticketCount());
        assertEquals(60.0, open.percentage());
    }

    @Test
    void testGetTeamWorkload() {
        UUID teamId = UUID.randomUUID();
        Team team = new Team("Support Team", "General");
        team.setId(teamId);

        when(teamRepository.findAll()).thenReturn(List.of(team));
        List<Object[]> teamRows = new ArrayList<>();
        teamRows.add(new Object[]{teamId, TicketStatus.OPEN, 2L});
        teamRows.add(new Object[]{teamId, TicketStatus.IN_PROGRESS, 1L});
        teamRows.add(new Object[]{teamId, TicketStatus.CLOSED, 3L});
        teamRows.add(new Object[]{teamId, TicketStatus.ESCALATED, 1L});
        when(ticketRepository.countTicketsByTeamAndStatus()).thenReturn(teamRows);

        List<TeamWorkloadResponse> result = analyticsService.getTeamWorkload();

        assertEquals(1, result.size());
        assertEquals("Support Team", result.get(0).teamName());
        assertEquals(3L, result.get(0).activeTickets());
        assertEquals(3L, result.get(0).resolvedClosedTickets());
        assertEquals(1L, result.get(0).escalatedTickets());
    }

    @Test
    void testGetAgentPerformance() {
        UUID agentId = UUID.randomUUID();
        User user = new User("agent@smartdesk.local", "hash", com.smartdesk.entity.enums.UserRole.AGENT);
        user.setProfile(new Profile(user, "John", "Doe", null, null));
        Team team = new Team("Tier 1", null);
        Agent agent = new Agent(user, team, "EMP-001", AgentAvailability.AVAILABLE, null, 5);
        agent.setId(agentId);

        when(agentRepository.findAll()).thenReturn(List.of(agent));
        List<Object[]> statusRows = new ArrayList<>();
        statusRows.add(new Object[]{agentId, TicketStatus.OPEN, 2L});
        statusRows.add(new Object[]{agentId, TicketStatus.RESOLVED, 3L});
        when(ticketRepository.countTicketsByAgentAndStatus()).thenReturn(statusRows);

        List<Object[]> resRows = new ArrayList<>();
        resRows.add(new Object[]{agentId, now.minusMinutes(60), now});
        when(ticketRepository.findAgentResolutionTimestamps()).thenReturn(resRows);

        List<AgentPerformanceResponse> result = analyticsService.getAgentPerformance();

        assertEquals(1, result.size());
        AgentPerformanceResponse metric = result.get(0);
        assertEquals("John Doe", metric.agentName());
        assertEquals("EMP-001", metric.employeeCode());
        assertEquals("Tier 1", metric.teamName());
        assertEquals(5L, metric.assignedTickets());
        assertEquals(2L, metric.openTickets());
        assertEquals(3L, metric.resolvedTickets());
        assertEquals(60.0, metric.avgResolutionMinutes());
    }

    @Test
    void testGetResolutionTimeStats() {
        List<Object[]> resRows = new ArrayList<>();
        resRows.add(new Object[]{now.minusMinutes(30), now});
        resRows.add(new Object[]{now.minusMinutes(60), now});
        resRows.add(new Object[]{now.minusMinutes(90), now});
        when(ticketRepository.findResolutionTimestamps()).thenReturn(resRows);

        ResolutionTimeStatsResponse stats = analyticsService.getResolutionTimeStats();

        assertEquals(3L, stats.resolvedCount());
        assertEquals(60.0, stats.avgResolutionMinutes());
        assertEquals(30.0, stats.minResolutionMinutes());
        assertEquals(90.0, stats.maxResolutionMinutes());
        assertEquals(60.0, stats.medianResolutionMinutes());
    }

    @Test
    void testGetResolutionTimeStatsEmpty() {
        when(ticketRepository.findResolutionTimestamps()).thenReturn(Collections.emptyList());

        ResolutionTimeStatsResponse stats = analyticsService.getResolutionTimeStats();

        assertEquals(0L, stats.resolvedCount());
        assertNull(stats.avgResolutionMinutes());
        assertNull(stats.minResolutionMinutes());
        assertNull(stats.maxResolutionMinutes());
        assertNull(stats.medianResolutionMinutes());
    }

    @Test
    void testGetEscalationStats() {
        when(escalationRepository.count()).thenReturn(3L);
        List<Object[]> levelRows = new ArrayList<>();
        levelRows.add(new Object[]{1, 2L});
        levelRows.add(new Object[]{2, 1L});
        when(escalationRepository.countGroupByLevel()).thenReturn(levelRows);

        List<Object[]> teamRows = new ArrayList<>();
        teamRows.add(new Object[]{"Tier 2", 3L});
        when(escalationRepository.countGroupByToTeam()).thenReturn(teamRows);

        Team toTeam = new Team("Tier 2", null);
        Ticket ticket = new Ticket();
        ticket.setTicketNumber("SD-100");
        Escalation esc = new Escalation(ticket, null, toTeam, null, null, 1, "Complex issue");
        esc.setStatus(EscalationStatus.OPEN);
        when(escalationRepository.findTop10ByOrderByCreatedAtDesc()).thenReturn(List.of(esc));

        EscalationStatsResponse res = analyticsService.getEscalationStats();

        assertEquals(3L, res.totalEscalations());
        assertEquals(2L, res.byLevel().get(1));
        assertEquals(1L, res.byLevel().get(2));
        assertEquals(3L, res.byTeam().get("Tier 2"));
        assertEquals(1, res.recentEscalations().size());
        assertEquals("SD-100", res.recentEscalations().get(0).ticketNumber());
    }

    @Test
    void testGetSlaAnalytics() {
        SlaPolicy p1 = new SlaPolicy("High SLA", TicketPriority.HIGH, 60, 240);
        when(slaPolicyRepository.findAll()).thenReturn(List.of(p1));
        List<Object[]> slaRows = new ArrayList<>();
        slaRows.add(new Object[]{TicketPriority.HIGH, now.minusMinutes(300)});
        when(ticketRepository.findActiveTicketsForSla()).thenReturn(slaRows);

        SlaAnalyticsResponse sla = analyticsService.getSlaAnalytics();

        assertEquals(1, sla.configuredPoliciesCount());
        assertEquals(1L, sla.ticketsPastResponseTargetCount());
        assertEquals(1L, sla.ticketsPastResolutionTargetCount());
        assertEquals(1, sla.policies().size());
    }
}
