package com.smartdesk.controller;

import com.smartdesk.dto.common.ApiResponse;
import com.smartdesk.dto.team.TeamResponse;
import com.smartdesk.service.TeamService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/teams")
public class TeamController {

    private final TeamService teamService;

    public TeamController(TeamService teamService) {
        this.teamService = teamService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<TeamResponse>>> getAllTeams() {
        List<TeamResponse> teams = teamService.getAllActiveTeams();
        return ResponseEntity.ok(ApiResponse.ok(teams));
    }
}
