package com.smartdesk.mapper;

import com.smartdesk.dto.agent.AgentResponse;
import com.smartdesk.dto.auth.UserResponse;
import com.smartdesk.dto.category.CategoryResponse;
import com.smartdesk.dto.customer.CustomerResponse;
import com.smartdesk.dto.escalation.EscalationResponse;
import com.smartdesk.dto.message.MessageResponse;
import com.smartdesk.dto.team.TeamResponse;
import com.smartdesk.dto.ticket.TicketDetailResponse;
import com.smartdesk.dto.ticket.TicketResponse;
import com.smartdesk.dto.ticket.TicketSummaryResponse;
import com.smartdesk.entity.*;

import java.util.Collections;
import java.util.List;

public final class EntityDtoMapper {

    private EntityDtoMapper() {}

    public static UserResponse toUserResponse(User user) {
        if (user == null) return null;
        String firstName = user.getProfile() != null ? user.getProfile().getFirstName() : null;
        String lastName = user.getProfile() != null ? user.getProfile().getLastName() : null;
        String customerCode = user.getCustomer() != null ? user.getCustomer().getCustomerCode() : null;
        String employeeCode = user.getAgent() != null ? user.getAgent().getEmployeeCode() : null;

        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getRole(),
                user.isActive(),
                firstName,
                lastName,
                customerCode,
                employeeCode,
                user.getCreatedAt()
        );
    }

    public static CustomerResponse toCustomerResponse(Customer customer) {
        if (customer == null) return null;
        String email = customer.getUser() != null ? customer.getUser().getEmail() : null;
        String firstName = (customer.getUser() != null && customer.getUser().getProfile() != null)
                ? customer.getUser().getProfile().getFirstName() : null;
        String lastName = (customer.getUser() != null && customer.getUser().getProfile() != null)
                ? customer.getUser().getProfile().getLastName() : null;

        return new CustomerResponse(
                customer.getId(),
                customer.getUser() != null ? customer.getUser().getId() : null,
                customer.getCustomerCode(),
                customer.getCompanyName(),
                customer.getPlan(),
                email,
                firstName,
                lastName,
                customer.getJoinedAt()
        );
    }

    public static AgentResponse toAgentResponse(Agent agent, long currentActiveTickets) {
        if (agent == null) return null;
        String email = agent.getUser() != null ? agent.getUser().getEmail() : null;
        String firstName = (agent.getUser() != null && agent.getUser().getProfile() != null)
                ? agent.getUser().getProfile().getFirstName() : null;
        String lastName = (agent.getUser() != null && agent.getUser().getProfile() != null)
                ? agent.getUser().getProfile().getLastName() : null;
        String teamName = agent.getTeam() != null ? agent.getTeam().getName() : null;

        return new AgentResponse(
                agent.getId(),
                agent.getUser() != null ? agent.getUser().getId() : null,
                agent.getEmployeeCode(),
                firstName,
                lastName,
                email,
                agent.getTeam() != null ? agent.getTeam().getId() : null,
                teamName,
                agent.getAvailabilityStatus(),
                agent.getSkills(),
                agent.getMaxActiveTickets(),
                currentActiveTickets
        );
    }

    public static TeamResponse toTeamResponse(Team team) {
        if (team == null) return null;
        int agentCount = team.getAgents() != null ? team.getAgents().size() : 0;
        return new TeamResponse(
                team.getId(),
                team.getName(),
                team.getDescription(),
                team.isActive(),
                agentCount
        );
    }

    public static CategoryResponse toCategoryResponse(Category category) {
        if (category == null) return null;
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.isActive()
        );
    }

    public static TicketResponse toTicketResponse(Ticket ticket) {
        if (ticket == null) return null;

        String customerName = null;
        String customerCode = null;
        if (ticket.getCustomer() != null) {
            customerCode = ticket.getCustomer().getCustomerCode();
            if (ticket.getCustomer().getUser() != null && ticket.getCustomer().getUser().getProfile() != null) {
                customerName = ticket.getCustomer().getUser().getProfile().getFirstName() + " " +
                               ticket.getCustomer().getUser().getProfile().getLastName();
            } else if (ticket.getCustomer().getCompanyName() != null) {
                customerName = ticket.getCustomer().getCompanyName();
            }
        }

        String categoryName = ticket.getCategory() != null ? ticket.getCategory().getName() : null;
        String agentName = null;
        if (ticket.getAssignedAgent() != null && ticket.getAssignedAgent().getUser() != null &&
            ticket.getAssignedAgent().getUser().getProfile() != null) {
            agentName = ticket.getAssignedAgent().getUser().getProfile().getFirstName() + " " +
                        ticket.getAssignedAgent().getUser().getProfile().getLastName();
        }
        String teamName = ticket.getAssignedTeam() != null ? ticket.getAssignedTeam().getName() : null;

        return new TicketResponse(
                ticket.getId(),
                ticket.getTicketNumber(),
                ticket.getCustomer() != null ? ticket.getCustomer().getId() : null,
                customerName,
                customerCode,
                ticket.getCategory() != null ? ticket.getCategory().getId() : null,
                categoryName,
                ticket.getAssignedAgent() != null ? ticket.getAssignedAgent().getId() : null,
                agentName,
                ticket.getAssignedTeam() != null ? ticket.getAssignedTeam().getId() : null,
                teamName,
                ticket.getSubject(),
                ticket.getDescription(),
                ticket.getPriority(),
                ticket.getStatus(),
                ticket.getAiCategory(),
                ticket.getAiConfidence(),
                ticket.getAiModelVersion(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt(),
                ticket.getResolvedAt(),
                ticket.getClosedAt()
        );
    }

    public static TicketSummaryResponse toTicketSummaryResponse(Ticket ticket) {
        if (ticket == null) return null;

        String customerName = null;
        if (ticket.getCustomer() != null) {
            if (ticket.getCustomer().getUser() != null && ticket.getCustomer().getUser().getProfile() != null) {
                customerName = ticket.getCustomer().getUser().getProfile().getFirstName() + " " +
                               ticket.getCustomer().getUser().getProfile().getLastName();
            } else {
                customerName = ticket.getCustomer().getCompanyName();
            }
        }
        String categoryName = ticket.getCategory() != null ? ticket.getCategory().getName() : null;
        String agentName = (ticket.getAssignedAgent() != null && ticket.getAssignedAgent().getUser() != null &&
                            ticket.getAssignedAgent().getUser().getProfile() != null)
                ? ticket.getAssignedAgent().getUser().getProfile().getFirstName() + " " + ticket.getAssignedAgent().getUser().getProfile().getLastName()
                : null;
        String teamName = ticket.getAssignedTeam() != null ? ticket.getAssignedTeam().getName() : null;

        return new TicketSummaryResponse(
                ticket.getId(),
                ticket.getTicketNumber(),
                ticket.getSubject(),
                customerName,
                categoryName,
                ticket.getPriority(),
                ticket.getStatus(),
                agentName,
                teamName,
                ticket.getCreatedAt()
        );
    }

    public static TicketDetailResponse toTicketDetailResponse(
            Ticket ticket,
            List<MessageResponse> messages,
            List<TicketDetailResponse.TicketEventDto> events
    ) {
        return toTicketDetailResponse(ticket, messages, events, null);
    }

    public static TicketDetailResponse toTicketDetailResponse(
            Ticket ticket,
            List<MessageResponse> messages,
            List<TicketDetailResponse.TicketEventDto> events,
            TicketSentimentAnalysis sentiment
    ) {
        return toTicketDetailResponse(ticket, messages, events, sentiment, null);
    }

    public static TicketDetailResponse toTicketDetailResponse(
            Ticket ticket,
            List<MessageResponse> messages,
            List<TicketDetailResponse.TicketEventDto> events,
            TicketSentimentAnalysis sentiment,
            List<TicketDetailResponse.DuplicateMatchDto> duplicateMatches
    ) {
        if (ticket == null) return null;
        TicketResponse base = toTicketResponse(ticket);

        return new TicketDetailResponse(
                base.id(),
                base.ticketNumber(),
                base.customerId(),
                base.customerName(),
                base.customerCode(),
                ticket.getCustomer() != null ? ticket.getCustomer().getCompanyName() : null,
                base.categoryId(),
                base.categoryName(),
                base.assignedAgentId(),
                base.assignedAgentName(),
                base.assignedTeamId(),
                base.assignedTeamName(),
                base.subject(),
                base.description(),
                base.priority(),
                base.status(),
                base.aiCategory(),
                base.aiConfidence(),
                base.aiModelVersion(),
                sentiment != null && sentiment.getSentiment() != null ? sentiment.getSentiment().name() : null,
                sentiment != null ? sentiment.getConfidence() : null,
                sentiment != null && sentiment.getTone() != null ? sentiment.getTone().name() : null,
                sentiment != null ? sentiment.getModelVersion() : null,
                base.createdAt(),
                base.updatedAt(),
                base.resolvedAt(),
                base.closedAt(),
                messages != null ? messages : Collections.emptyList(),
                events != null ? events : Collections.emptyList(),
                duplicateMatches
        );
    }


    public static MessageResponse toMessageResponse(TicketMessage msg) {
        if (msg == null) return null;
        String senderName = null;
        if (msg.getSender() != null && msg.getSender().getProfile() != null) {
            senderName = msg.getSender().getProfile().getFirstName() + " " + msg.getSender().getProfile().getLastName();
        } else if (msg.getSender() != null) {
            senderName = msg.getSender().getEmail();
        }

        return new MessageResponse(
                msg.getId(),
                msg.getTicket() != null ? msg.getTicket().getId() : null,
                msg.getSender() != null ? msg.getSender().getId() : null,
                senderName,
                msg.getSender() != null ? msg.getSender().getRole() : null,
                msg.getMessage(),
                msg.isInternal(),
                msg.getCreatedAt()
        );
    }

    public static EscalationResponse toEscalationResponse(Escalation e) {
        if (e == null) return null;

        String ticketNumber = e.getTicket() != null ? e.getTicket().getTicketNumber() : null;
        String fromTeamName = e.getFromTeam() != null ? e.getFromTeam().getName() : null;
        String toTeamName = e.getToTeam() != null ? e.getToTeam().getName() : null;
        String fromAgentName = (e.getFromAgent() != null && e.getFromAgent().getUser() != null && e.getFromAgent().getUser().getProfile() != null)
                ? e.getFromAgent().getUser().getProfile().getFirstName() + " " + e.getFromAgent().getUser().getProfile().getLastName() : null;
        String toAgentName = (e.getToAgent() != null && e.getToAgent().getUser() != null && e.getToAgent().getUser().getProfile() != null)
                ? e.getToAgent().getUser().getProfile().getFirstName() + " " + e.getToAgent().getUser().getProfile().getLastName() : null;

        return new EscalationResponse(
                e.getId(),
                e.getTicket() != null ? e.getTicket().getId() : null,
                ticketNumber,
                e.getFromTeam() != null ? e.getFromTeam().getId() : null,
                fromTeamName,
                e.getToTeam() != null ? e.getToTeam().getId() : null,
                toTeamName,
                e.getFromAgent() != null ? e.getFromAgent().getId() : null,
                fromAgentName,
                e.getToAgent() != null ? e.getToAgent().getId() : null,
                toAgentName,
                e.getLevel(),
                e.getReason(),
                e.getStatus(),
                e.getCreatedAt(),
                e.getResolvedAt()
        );
    }
}
