package com.smartdesk.service;

import com.smartdesk.dto.analytics.*;
import com.smartdesk.dto.analytics.EscalationStatsResponse.EscalationSummaryDto;
import com.smartdesk.dto.analytics.SlaAnalyticsResponse.SlaPolicyMetricDto;
import com.smartdesk.entity.Agent;
import com.smartdesk.entity.Category;
import com.smartdesk.entity.Escalation;
import com.smartdesk.entity.SlaPolicy;
import com.smartdesk.entity.Team;
import com.smartdesk.entity.enums.TicketPriority;
import com.smartdesk.entity.enums.TicketStatus;
import com.smartdesk.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {

    private final TicketRepository ticketRepository;
    private final CategoryRepository categoryRepository;
    private final TeamRepository teamRepository;
    private final AgentRepository agentRepository;
    private final EscalationRepository escalationRepository;
    private final SlaPolicyRepository slaPolicyRepository;

    public AnalyticsService(
            TicketRepository ticketRepository,
            CategoryRepository categoryRepository,
            TeamRepository teamRepository,
            AgentRepository agentRepository,
            EscalationRepository escalationRepository,
            SlaPolicyRepository slaPolicyRepository
    ) {
        this.ticketRepository = ticketRepository;
        this.categoryRepository = categoryRepository;
        this.teamRepository = teamRepository;
        this.agentRepository = agentRepository;
        this.escalationRepository = escalationRepository;
        this.slaPolicyRepository = slaPolicyRepository;
    }

    @Transactional(readOnly = true)
    public AnalyticsOverviewResponse getOverview() {
        long total = ticketRepository.count();
        long open = ticketRepository.countByStatus(TicketStatus.OPEN);
        long inProgress = ticketRepository.countByStatus(TicketStatus.IN_PROGRESS);
        long pendingCustomer = ticketRepository.countByStatus(TicketStatus.PENDING_CUSTOMER);
        long pendingInternal = ticketRepository.countByStatus(TicketStatus.PENDING_INTERNAL);
        long pending = pendingCustomer + pendingInternal;
        long escalated = ticketRepository.countByStatus(TicketStatus.ESCALATED);
        long resolved = ticketRepository.countByStatus(TicketStatus.RESOLVED);
        long closed = ticketRepository.countByStatus(TicketStatus.CLOSED);

        Double avgResTime = calculateAvgResolutionMinutes();

        return new AnalyticsOverviewResponse(total, open, inProgress, pending, escalated, resolved, closed, avgResTime);
    }

    @Transactional(readOnly = true)
    public List<TicketVolumeResponse> getTicketsOverTime(Integer days) {
        LocalDate endDate = LocalDate.now(ZoneOffset.UTC);
        LocalDate startDate;

        List<Object[]> rows;
        if (days != null && days > 0) {
            startDate = endDate.minusDays(days - 1L);
            OffsetDateTime startOffset = startDate.atStartOfDay().atOffset(ZoneOffset.UTC);
            rows = ticketRepository.countTicketsByDaySince(startOffset);
        } else {
            rows = ticketRepository.countTicketsByDayAllTime();
            if (rows.isEmpty()) {
                startDate = endDate.minusDays(29);
            } else {
                startDate = parseRowDate(rows.get(0)[0]);
            }
        }

        Map<LocalDate, Long> countsByDate = new HashMap<>();
        for (Object[] row : rows) {
            LocalDate d = parseRowDate(row[0]);
            long cnt = ((Number) row[1]).longValue();
            countsByDate.put(d, cnt);
        }

        List<TicketVolumeResponse> result = new ArrayList<>();
        for (LocalDate curr = startDate; !curr.isAfter(endDate); curr = curr.plusDays(1)) {
            result.add(new TicketVolumeResponse(curr.toString(), countsByDate.getOrDefault(curr, 0L)));
        }

        return result;
    }

    @Transactional(readOnly = true)
    public List<CategoryStatsResponse> getTicketsByCategory() {
        long total = ticketRepository.count();
        List<Category> categories = categoryRepository.findAll();
        List<Object[]> rows = ticketRepository.countGroupByAssignedCategory();

        Map<UUID, Long> countMap = new HashMap<>();
        for (Object[] row : rows) {
            UUID catId = (UUID) row[0];
            long count = ((Number) row[2]).longValue();
            countMap.put(catId, count);
        }

        List<CategoryStatsResponse> results = new ArrayList<>();
        for (Category category : categories) {
            long count = countMap.getOrDefault(category.getId(), 0L);
            double percentage = total > 0 ? Math.round((count * 1000.0 / total)) / 10.0 : 0.0;
            results.add(new CategoryStatsResponse(category.getId(), category.getName(), count, percentage));
        }

        return results;
    }

    @Transactional(readOnly = true)
    public List<PriorityStatsResponse> getTicketsByPriority() {
        long total = ticketRepository.count();
        List<Object[]> rows = ticketRepository.countGroupByPriority();

        Map<TicketPriority, Long> countMap = new HashMap<>();
        for (Object[] row : rows) {
            TicketPriority priority = (TicketPriority) row[0];
            long count = ((Number) row[1]).longValue();
            countMap.put(priority, count);
        }

        List<PriorityStatsResponse> results = new ArrayList<>();
        for (TicketPriority priority : TicketPriority.values()) {
            long count = countMap.getOrDefault(priority, 0L);
            double percentage = total > 0 ? Math.round((count * 1000.0 / total)) / 10.0 : 0.0;
            results.add(new PriorityStatsResponse(priority, count, percentage));
        }

        return results;
    }

    @Transactional(readOnly = true)
    public List<StatusStatsResponse> getTicketsByStatus() {
        long total = ticketRepository.count();
        List<Object[]> rows = ticketRepository.countGroupByStatus();

        Map<TicketStatus, Long> countMap = new HashMap<>();
        for (Object[] row : rows) {
            TicketStatus status = (TicketStatus) row[0];
            long count = ((Number) row[1]).longValue();
            countMap.put(status, count);
        }

        List<StatusStatsResponse> results = new ArrayList<>();
        for (TicketStatus status : TicketStatus.values()) {
            long count = countMap.getOrDefault(status, 0L);
            double percentage = total > 0 ? Math.round((count * 1000.0 / total)) / 10.0 : 0.0;
            results.add(new StatusStatsResponse(status, count, percentage));
        }

        return results;
    }

    @Transactional(readOnly = true)
    public List<TeamWorkloadResponse> getTeamWorkload() {
        List<Team> teams = teamRepository.findAll();
        List<Object[]> rows = ticketRepository.countTicketsByTeamAndStatus();

        // teamId -> (status -> count)
        Map<UUID, Map<TicketStatus, Long>> teamStatusCounts = new HashMap<>();
        for (Object[] row : rows) {
            UUID teamId = (UUID) row[0];
            TicketStatus status = (TicketStatus) row[1];
            long count = ((Number) row[2]).longValue();

            teamStatusCounts.computeIfAbsent(teamId, k -> new HashMap<>()).put(status, count);
        }

        List<TeamWorkloadResponse> results = new ArrayList<>();
        for (Team team : teams) {
            Map<TicketStatus, Long> counts = teamStatusCounts.getOrDefault(team.getId(), Collections.emptyMap());

            long active = counts.getOrDefault(TicketStatus.OPEN, 0L)
                    + counts.getOrDefault(TicketStatus.IN_PROGRESS, 0L)
                    + counts.getOrDefault(TicketStatus.PENDING_CUSTOMER, 0L)
                    + counts.getOrDefault(TicketStatus.PENDING_INTERNAL, 0L);

            long resolvedClosed = counts.getOrDefault(TicketStatus.RESOLVED, 0L)
                    + counts.getOrDefault(TicketStatus.CLOSED, 0L);

            long escalated = counts.getOrDefault(TicketStatus.ESCALATED, 0L);

            results.add(new TeamWorkloadResponse(
                    team.getId(),
                    team.getName(),
                    active,
                    resolvedClosed,
                    escalated
            ));
        }

        return results;
    }

    @Transactional(readOnly = true)
    public List<AgentPerformanceResponse> getAgentPerformance() {
        List<Agent> agents = agentRepository.findAll();
        List<Object[]> statusRows = ticketRepository.countTicketsByAgentAndStatus();
        List<Object[]> resolutionRows = ticketRepository.findAgentResolutionTimestamps();

        // agentId -> (status -> count)
        Map<UUID, Map<TicketStatus, Long>> agentStatusCounts = new HashMap<>();
        for (Object[] row : statusRows) {
            UUID agentId = (UUID) row[0];
            TicketStatus status = (TicketStatus) row[1];
            long count = ((Number) row[2]).longValue();

            agentStatusCounts.computeIfAbsent(agentId, k -> new HashMap<>()).put(status, count);
        }

        // agentId -> list of resolution minutes
        Map<UUID, List<Long>> agentDurations = new HashMap<>();
        for (Object[] row : resolutionRows) {
            UUID agentId = (UUID) row[0];
            OffsetDateTime created = (OffsetDateTime) row[1];
            OffsetDateTime resolved = (OffsetDateTime) row[2];
            long mins = Math.max(0L, Duration.between(created, resolved).toMinutes());
            agentDurations.computeIfAbsent(agentId, k -> new ArrayList<>()).add(mins);
        }

        List<AgentPerformanceResponse> results = new ArrayList<>();
        for (Agent agent : agents) {
            Map<TicketStatus, Long> counts = agentStatusCounts.getOrDefault(agent.getId(), Collections.emptyMap());

            long assigned = counts.values().stream().mapToLong(Long::longValue).sum();
            long open = counts.getOrDefault(TicketStatus.OPEN, 0L)
                    + counts.getOrDefault(TicketStatus.IN_PROGRESS, 0L)
                    + counts.getOrDefault(TicketStatus.PENDING_CUSTOMER, 0L)
                    + counts.getOrDefault(TicketStatus.PENDING_INTERNAL, 0L)
                    + counts.getOrDefault(TicketStatus.ESCALATED, 0L);

            long resolved = counts.getOrDefault(TicketStatus.RESOLVED, 0L)
                    + counts.getOrDefault(TicketStatus.CLOSED, 0L);

            List<Long> durations = agentDurations.getOrDefault(agent.getId(), Collections.emptyList());
            Double avgMinutes = durations.isEmpty() ? null :
                    Math.round(durations.stream().mapToLong(Long::longValue).average().orElse(0.0) * 10.0) / 10.0;

            String agentName = "Unknown Agent";
            if (agent.getUser() != null) {
                if (agent.getUser().getProfile() != null) {
                    agentName = agent.getUser().getProfile().getFirstName() + " " + agent.getUser().getProfile().getLastName();
                } else {
                    agentName = agent.getUser().getEmail();
                }
            }

            String teamName = agent.getTeam() != null ? agent.getTeam().getName() : "Unassigned";

            results.add(new AgentPerformanceResponse(
                    agent.getId(),
                    agentName,
                    agent.getEmployeeCode(),
                    teamName,
                    assigned,
                    open,
                    resolved,
                    avgMinutes
            ));
        }

        return results;
    }

    @Transactional(readOnly = true)
    public ResolutionTimeStatsResponse getResolutionTimeStats() {
        List<Object[]> rows = ticketRepository.findResolutionTimestamps();
        if (rows.isEmpty()) {
            return new ResolutionTimeStatsResponse(null, null, null, null, 0L);
        }

        List<Long> durations = new ArrayList<>();
        for (Object[] row : rows) {
            OffsetDateTime created = (OffsetDateTime) row[0];
            OffsetDateTime resolved = (OffsetDateTime) row[1];
            durations.add(Math.max(0L, Duration.between(created, resolved).toMinutes()));
        }

        Collections.sort(durations);

        long count = durations.size();
        double avg = Math.round(durations.stream().mapToLong(Long::longValue).average().orElse(0.0) * 10.0) / 10.0;
        double min = durations.get(0).doubleValue();
        double max = durations.get(durations.size() - 1).doubleValue();

        double median;
        int size = durations.size();
        if (size % 2 == 1) {
            median = durations.get(size / 2).doubleValue();
        } else {
            median = (durations.get((size / 2) - 1) + durations.get(size / 2)) / 2.0;
        }
        median = Math.round(median * 10.0) / 10.0;

        return new ResolutionTimeStatsResponse(avg, min, max, median, count);
    }

    @Transactional(readOnly = true)
    public EscalationStatsResponse getEscalationStats() {
        long total = escalationRepository.count();

        List<Object[]> levelRows = escalationRepository.countGroupByLevel();
        Map<Integer, Long> byLevel = new HashMap<>();
        for (Object[] row : levelRows) {
            int level = ((Number) row[0]).intValue();
            long cnt = ((Number) row[1]).longValue();
            byLevel.put(level, cnt);
        }

        List<Object[]> teamRows = escalationRepository.countGroupByToTeam();
        Map<String, Long> byTeam = new HashMap<>();
        for (Object[] row : teamRows) {
            String teamName = (String) row[0];
            long cnt = ((Number) row[1]).longValue();
            byTeam.put(teamName, cnt);
        }

        List<Escalation> recent = escalationRepository.findTop10ByOrderByCreatedAtDesc();
        List<EscalationSummaryDto> recentDtos = recent.stream().map(e -> new EscalationSummaryDto(
                e.getId(),
                e.getTicket() != null ? e.getTicket().getTicketNumber() : "Unknown",
                e.getLevel(),
                e.getFromTeam() != null ? e.getFromTeam().getName() : "None",
                e.getToTeam() != null ? e.getToTeam().getName() : "Unknown",
                e.getReason(),
                e.getStatus().name(),
                e.getCreatedAt()
        )).collect(Collectors.toList());

        return new EscalationStatsResponse(total, byLevel, byTeam, recentDtos);
    }

    @Transactional(readOnly = true)
    public SlaAnalyticsResponse getSlaAnalytics() {
        List<SlaPolicy> policies = slaPolicyRepository.findAll();
        List<SlaPolicyMetricDto> policyDtos = policies.stream().map(p -> new SlaPolicyMetricDto(
                p.getId(),
                p.getName(),
                p.getPriority(),
                p.getFirstResponseMinutes(),
                p.getResolutionMinutes(),
                p.isActive()
        )).collect(Collectors.toList());

        Map<TicketPriority, SlaPolicy> policyByPriority = policies.stream()
                .filter(SlaPolicy::isActive)
                .collect(Collectors.toMap(SlaPolicy::getPriority, p -> p, (a, b) -> a));

        List<Object[]> activeTickets = ticketRepository.findActiveTicketsForSla();

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        long pastResponseCount = 0;
        long pastResolutionCount = 0;

        for (Object[] row : activeTickets) {
            TicketPriority priority = (TicketPriority) row[0];
            OffsetDateTime createdAt = (OffsetDateTime) row[1];
            SlaPolicy policy = policyByPriority.get(priority);
            if (policy != null) {
                long elapsed = Duration.between(createdAt, now).toMinutes();
                if (elapsed > policy.getFirstResponseMinutes()) {
                    pastResponseCount++;
                }
                if (elapsed > policy.getResolutionMinutes()) {
                    pastResolutionCount++;
                }
            }
        }

        return new SlaAnalyticsResponse(
                policies.size(),
                pastResponseCount,
                pastResolutionCount,
                policyDtos
        );
    }

    private Double calculateAvgResolutionMinutes() {
        List<Object[]> rows = ticketRepository.findResolutionTimestamps();
        if (rows.isEmpty()) {
            return null;
        }
        double avg = rows.stream()
                .mapToLong(r -> Math.max(0L, Duration.between((OffsetDateTime) r[0], (OffsetDateTime) r[1]).toMinutes()))
                .average()
                .orElse(0.0);
        return Math.round(avg * 10.0) / 10.0;
    }

    private LocalDate parseRowDate(Object obj) {
        if (obj instanceof LocalDate ld) {
            return ld;
        } else if (obj instanceof java.sql.Date sd) {
            return sd.toLocalDate();
        } else if (obj instanceof OffsetDateTime odt) {
            return odt.toLocalDate();
        }
        return LocalDate.parse(obj.toString());
    }
}
