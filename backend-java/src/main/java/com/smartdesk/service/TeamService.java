package com.smartdesk.service;

import com.smartdesk.dto.team.TeamResponse;
import com.smartdesk.entity.Team;
import com.smartdesk.exception.ResourceNotFoundException;
import com.smartdesk.mapper.EntityDtoMapper;
import com.smartdesk.repository.TeamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class TeamService {

    private final TeamRepository teamRepository;

    public TeamService(TeamRepository teamRepository) {
        this.teamRepository = teamRepository;
    }

    @Transactional(readOnly = true)
    public List<TeamResponse> getAllActiveTeams() {
        return teamRepository.findByIsActiveTrue()
                .stream()
                .map(EntityDtoMapper::toTeamResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Team getTeamEntity(UUID id) {
        return teamRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found with ID: " + id));
    }
}
