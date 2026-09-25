package com.smartdesk.service;

import com.smartdesk.dto.escalation.EscalationRequest;
import com.smartdesk.dto.escalation.EscalationResponse;
import com.smartdesk.entity.*;
import com.smartdesk.entity.enums.EventType;
import com.smartdesk.entity.enums.TicketStatus;
import com.smartdesk.exception.BadRequestException;
import com.smartdesk.exception.ResourceNotFoundException;
import com.smartdesk.mapper.EntityDtoMapper;
import com.smartdesk.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class EscalationService {

    private final EscalationRepository escalationRepository;
    private final TicketRepository ticketRepository;
    private final TeamRepository teamRepository;
    private final AgentRepository agentRepository;
    private final TicketEventRepository ticketEventRepository;
    private final TicketAssignmentRepository ticketAssignmentRepository;
    private final AuditLogService auditLogService;

    public EscalationService(
            EscalationRepository escalationRepository,
            TicketRepository ticketRepository,
            TeamRepository teamRepository,
            AgentRepository agentRepository,
            TicketEventRepository ticketEventRepository,
            TicketAssignmentRepository ticketAssignmentRepository,
            AuditLogService auditLogService
    ) {
        this.escalationRepository = escalationRepository;
        this.ticketRepository = ticketRepository;
        this.teamRepository = teamRepository;
        this.agentRepository = agentRepository;
        this.ticketEventRepository = ticketEventRepository;
        this.ticketAssignmentRepository = ticketAssignmentRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public EscalationResponse escalateTicket(UUID ticketId, EscalationRequest request, User currentUser) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with ID: " + ticketId));

        if (ticket.getStatus() == TicketStatus.CLOSED) {
            throw new BadRequestException("Closed tickets cannot be escalated");
        }

        Team toTeam = teamRepository.findById(request.targetTeamId())
                .orElseThrow(() -> new ResourceNotFoundException("Target team not found with ID: " + request.targetTeamId()));

        Agent toAgent = null;
        if (request.targetAgentId() != null) {
            toAgent = agentRepository.findById(request.targetAgentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Target agent not found with ID: " + request.targetAgentId()));
        }

        Team fromTeam = ticket.getAssignedTeam();
        Agent fromAgent = ticket.getAssignedAgent();

        // 1. Create Escalation record
        Escalation escalation = new Escalation(
                ticket,
                fromTeam,
                toTeam,
                fromAgent,
                toAgent,
                request.level() > 0 ? request.level() : 1,
                request.reason()
        );
        escalation = escalationRepository.save(escalation);

        // 2. Transition ticket to ESCALATED status and reassign team
        TicketStatus oldStatus = ticket.getStatus();
        ticket.setStatus(TicketStatus.ESCALATED);
        ticket.setAssignedTeam(toTeam);
        ticket.setAssignedAgent(toAgent);
        ticketRepository.save(ticket);

        // 3. Record assignment and timeline events
        TicketAssignment assignment = new TicketAssignment(
                ticket,
                toAgent,
                toTeam,
                currentUser,
                "Ticket Escalation: " + request.reason()
        );
        ticketAssignmentRepository.save(assignment);

        TicketEvent event = new TicketEvent(
                ticket,
                currentUser,
                EventType.ESCALATED,
                oldStatus.name(),
                TicketStatus.ESCALATED.name(),
                "{\"escalationId\": \"" + escalation.getId() + "\", \"toTeam\": \"" + toTeam.getName() + "\"}"
        );
        ticketEventRepository.save(event);

        auditLogService.logAction(currentUser, "TICKET_ESCALATED", "Ticket", ticket.getId().toString(), null, toTeam.getName(), null, null);

        return EntityDtoMapper.toEscalationResponse(escalation);
    }
}
