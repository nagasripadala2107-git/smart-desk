package com.smartdesk.escalation;

import com.smartdesk.entity.Team;
import com.smartdesk.repository.EscalationRuleRepository;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Service boundary for Multi-Tier Escalation Paths.
 * In Phase 3, this provides rule lookups for team transitions.
 * In Phase 7, this will be extended with graph algorithms (BFS/DFS/Shortest Path)
 * across the organization's escalation DAG.
 */
@Service
public class EscalationPathService {

    private final EscalationRuleRepository escalationRuleRepository;

    public EscalationPathService(EscalationRuleRepository escalationRuleRepository) {
        this.escalationRuleRepository = escalationRuleRepository;
    }

    public List<Team> getAvailableEscalationTargets(UUID currentTeamId) {
        if (currentTeamId == null) {
            return Collections.emptyList();
        }
        return escalationRuleRepository.findByFromTeamIdAndIsActiveTrue(currentTeamId)
                .stream()
                .map(rule -> rule.getToTeam())
                .distinct()
                .toList();
    }
}
