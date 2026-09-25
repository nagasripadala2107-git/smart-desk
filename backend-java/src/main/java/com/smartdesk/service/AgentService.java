package com.smartdesk.service;

import com.smartdesk.dto.agent.AgentQueueResponse;
import com.smartdesk.dto.agent.AgentResponse;
import com.smartdesk.dto.ticket.TicketSummaryResponse;
import com.smartdesk.entity.Agent;
import com.smartdesk.entity.enums.TicketStatus;
import com.smartdesk.exception.ResourceNotFoundException;
import com.smartdesk.mapper.EntityDtoMapper;
import com.smartdesk.repository.AgentRepository;
import com.smartdesk.repository.TicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class AgentService {

    private final AgentRepository agentRepository;
    private final TicketRepository ticketRepository;

    public AgentService(AgentRepository agentRepository, TicketRepository ticketRepository) {
        this.agentRepository = agentRepository;
        this.ticketRepository = ticketRepository;
    }

    @Transactional(readOnly = true)
    public Agent getAgentByUserId(UUID userId) {
        return agentRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Agent record not found for user ID: " + userId));
    }

    @Transactional(readOnly = true)
    public Agent getAgentEntity(UUID id) {
        return agentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Agent not found with ID: " + id));
    }

    @Transactional(readOnly = true)
    public AgentResponse getAgentDtoByUserId(UUID userId) {
        Agent agent = getAgentByUserId(userId);
        long activeCount = ticketRepository.countByAssignedAgentIdAndStatusIn(
                agent.getId(),
                Arrays.asList(TicketStatus.OPEN, TicketStatus.IN_PROGRESS, TicketStatus.PENDING_CUSTOMER, TicketStatus.PENDING_INTERNAL, TicketStatus.ESCALATED)
        );
        return EntityDtoMapper.toAgentResponse(agent, activeCount);
    }

    @Transactional(readOnly = true)
    public AgentQueueResponse getAgentQueue(UUID userId) {
        Agent agent = getAgentByUserId(userId);

        List<TicketSummaryResponse> assigned = ticketRepository.findByAssignedAgentIdOrderByCreatedAtDesc(agent.getId())
                .stream()
                .filter(t -> t.getStatus() != TicketStatus.RESOLVED && t.getStatus() != TicketStatus.CLOSED)
                .map(EntityDtoMapper::toTicketSummaryResponse)
                .toList();

        List<TicketSummaryResponse> teamQueue = agent.getTeam() != null
                ? ticketRepository.findByAssignedTeamIdOrderByCreatedAtDesc(agent.getTeam().getId())
                    .stream()
                    .filter(t -> t.getAssignedAgent() == null && t.getStatus() != TicketStatus.RESOLVED && t.getStatus() != TicketStatus.CLOSED)
                    .map(EntityDtoMapper::toTicketSummaryResponse)
                    .toList()
                : List.of();

        String agentName = (agent.getUser() != null && agent.getUser().getProfile() != null)
                ? agent.getUser().getProfile().getFirstName() + " " + agent.getUser().getProfile().getLastName()
                : agent.getEmployeeCode();

        return new AgentQueueResponse(
                agent.getId(),
                agent.getEmployeeCode(),
                agentName,
                agent.getTeam() != null ? agent.getTeam().getId() : null,
                agent.getTeam() != null ? agent.getTeam().getName() : null,
                assigned.size(),
                assigned,
                teamQueue
        );
    }
}
