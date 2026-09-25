package com.smartdesk.controller;

import com.smartdesk.dto.analytics.*;
import com.smartdesk.dto.common.ApiResponse;
import com.smartdesk.service.AnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/analytics")
@PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<AnalyticsOverviewResponse>> getOverview() {
        AnalyticsOverviewResponse overview = analyticsService.getOverview();
        return ResponseEntity.ok(ApiResponse.ok(overview));
    }

    @GetMapping("/tickets-over-time")
    public ResponseEntity<ApiResponse<List<TicketVolumeResponse>>> getTicketsOverTime(
            @RequestParam(name = "days", required = false, defaultValue = "30") Integer days
    ) {
        List<TicketVolumeResponse> volume = analyticsService.getTicketsOverTime(days);
        return ResponseEntity.ok(ApiResponse.ok(volume));
    }

    @GetMapping("/tickets-by-category")
    public ResponseEntity<ApiResponse<List<CategoryStatsResponse>>> getTicketsByCategory() {
        List<CategoryStatsResponse> stats = analyticsService.getTicketsByCategory();
        return ResponseEntity.ok(ApiResponse.ok(stats));
    }

    @GetMapping("/tickets-by-priority")
    public ResponseEntity<ApiResponse<List<PriorityStatsResponse>>> getTicketsByPriority() {
        List<PriorityStatsResponse> stats = analyticsService.getTicketsByPriority();
        return ResponseEntity.ok(ApiResponse.ok(stats));
    }

    @GetMapping("/tickets-by-status")
    public ResponseEntity<ApiResponse<List<StatusStatsResponse>>> getTicketsByStatus() {
        List<StatusStatsResponse> stats = analyticsService.getTicketsByStatus();
        return ResponseEntity.ok(ApiResponse.ok(stats));
    }

    @GetMapping("/team-performance")
    public ResponseEntity<ApiResponse<List<TeamWorkloadResponse>>> getTeamPerformance() {
        List<TeamWorkloadResponse> performance = analyticsService.getTeamWorkload();
        return ResponseEntity.ok(ApiResponse.ok(performance));
    }

    @GetMapping("/agent-performance")
    public ResponseEntity<ApiResponse<List<AgentPerformanceResponse>>> getAgentPerformance() {
        List<AgentPerformanceResponse> performance = analyticsService.getAgentPerformance();
        return ResponseEntity.ok(ApiResponse.ok(performance));
    }

    @GetMapping("/resolution-time")
    public ResponseEntity<ApiResponse<ResolutionTimeStatsResponse>> getResolutionTime() {
        ResolutionTimeStatsResponse stats = analyticsService.getResolutionTimeStats();
        return ResponseEntity.ok(ApiResponse.ok(stats));
    }

    @GetMapping("/escalations")
    public ResponseEntity<ApiResponse<EscalationStatsResponse>> getEscalations() {
        EscalationStatsResponse stats = analyticsService.getEscalationStats();
        return ResponseEntity.ok(ApiResponse.ok(stats));
    }

    @GetMapping("/sla")
    public ResponseEntity<ApiResponse<SlaAnalyticsResponse>> getSla() {
        SlaAnalyticsResponse sla = analyticsService.getSlaAnalytics();
        return ResponseEntity.ok(ApiResponse.ok(sla));
    }
}
