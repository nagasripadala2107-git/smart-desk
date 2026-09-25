package com.smartdesk.service;

import com.smartdesk.client.ai.ClassificationResponse;
import com.smartdesk.dto.ticket.*;
import com.smartdesk.entity.*;
import com.smartdesk.entity.enums.EventType;
import com.smartdesk.entity.enums.TicketStatus;
import com.smartdesk.entity.enums.UserRole;
import com.smartdesk.exception.BadRequestException;
import com.smartdesk.exception.ForbiddenException;
import com.smartdesk.exception.ResourceNotFoundException;
import com.smartdesk.mapper.EntityDtoMapper;
import com.smartdesk.repository.*;
import com.smartdesk.routing.TicketRoutingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class TicketService {

    private final TicketRepository ticketRepository;
    private final CustomerRepository customerRepository;
    private final CategoryRepository categoryRepository;
    private final AgentRepository agentRepository;
    private final TeamRepository teamRepository;
    private final TicketEventRepository ticketEventRepository;
    private final TicketAssignmentRepository ticketAssignmentRepository;
    private final TicketMessageRepository ticketMessageRepository;
    private final TicketNumberGenerator ticketNumberGenerator;
    private final TicketRoutingService routingService;
    private final TicketClassificationService classificationService;
    private final AuditLogService auditLogService;
    private final TicketSentimentService sentimentService;
    private final TicketDuplicateService duplicateService;

    public TicketService(
            TicketRepository ticketRepository,
            CustomerRepository customerRepository,
            CategoryRepository categoryRepository,
            AgentRepository agentRepository,
            TeamRepository teamRepository,
            TicketEventRepository ticketEventRepository,
            TicketAssignmentRepository ticketAssignmentRepository,
            TicketMessageRepository ticketMessageRepository,
            TicketNumberGenerator ticketNumberGenerator,
            TicketRoutingService routingService,
            TicketClassificationService classificationService,
            AuditLogService auditLogService
    ) {
        this(ticketRepository, customerRepository, categoryRepository, agentRepository,
                teamRepository, ticketEventRepository, ticketAssignmentRepository,
                ticketMessageRepository, ticketNumberGenerator, routingService,
                classificationService, auditLogService, null, null);
    }

    public TicketService(
            TicketRepository ticketRepository,
            CustomerRepository customerRepository,
            CategoryRepository categoryRepository,
            AgentRepository agentRepository,
            TeamRepository teamRepository,
            TicketEventRepository ticketEventRepository,
            TicketAssignmentRepository ticketAssignmentRepository,
            TicketMessageRepository ticketMessageRepository,
            TicketNumberGenerator ticketNumberGenerator,
            TicketRoutingService routingService,
            TicketClassificationService classificationService,
            AuditLogService auditLogService,
            TicketSentimentService sentimentService
    ) {
        this(ticketRepository, customerRepository, categoryRepository, agentRepository,
                teamRepository, ticketEventRepository, ticketAssignmentRepository,
                ticketMessageRepository, ticketNumberGenerator, routingService,
                classificationService, auditLogService, sentimentService, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public TicketService(
            TicketRepository ticketRepository,
            CustomerRepository customerRepository,
            CategoryRepository categoryRepository,
            AgentRepository agentRepository,
            TeamRepository teamRepository,
            TicketEventRepository ticketEventRepository,
            TicketAssignmentRepository ticketAssignmentRepository,
            TicketMessageRepository ticketMessageRepository,
            TicketNumberGenerator ticketNumberGenerator,
            TicketRoutingService routingService,
            TicketClassificationService classificationService,
            AuditLogService auditLogService,
            TicketSentimentService sentimentService,
            TicketDuplicateService duplicateService
    ) {
        this.ticketRepository = ticketRepository;
        this.customerRepository = customerRepository;
        this.categoryRepository = categoryRepository;
        this.agentRepository = agentRepository;
        this.teamRepository = teamRepository;
        this.ticketEventRepository = ticketEventRepository;
        this.ticketAssignmentRepository = ticketAssignmentRepository;
        this.ticketMessageRepository = ticketMessageRepository;
        this.ticketNumberGenerator = ticketNumberGenerator;
        this.routingService = routingService;
        this.classificationService = classificationService;
        this.auditLogService = auditLogService;
        this.sentimentService = sentimentService;
        this.duplicateService = duplicateService;
    }


    @Transactional
    public TicketResponse createTicket(CreateTicketRequest request, User customerUser) {
        Customer customer = customerRepository.findByUserId(customerUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer profile not found for user: " + customerUser.getEmail()));

        Category category = null;
        if (request.categoryId() != null) {
            category = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + request.categoryId()));
        }

        // 1. Generate unique ticket number (SD-YYYY-XXXXXX)
        String ticketNumber = ticketNumberGenerator.generateNextTicketNumber();

        // 2. Create ticket with initial status OPEN
        Ticket ticket = new Ticket(
                ticketNumber,
                customer,
                category,
                request.subject(),
                request.description(),
                request.priority()
        );

        // 3. AI Ticket Classification (Python microservice integration)
        Optional<ClassificationResponse> aiResult = classificationService.classifyTicket(request.subject(), request.description());
        if (aiResult.isPresent()) {
            ClassificationResponse ai = aiResult.get();
            ticket.setAiCategory(ai.category());
            if (ai.confidence() != null) {
                ticket.setAiConfidence(BigDecimal.valueOf(ai.confidence()).setScale(4, RoundingMode.HALF_UP));
            }
            if (ai.modelVersion() != null) {
                ticket.setAiModelVersion(ai.modelVersion());
            }

            // If explicit category was not supplied by customer, apply the AI recommended category
            if (category == null && ai.category() != null) {
                Optional<Category> aiCategoryEntity = categoryRepository.findByName(ai.category());
                if (aiCategoryEntity.isPresent()) {
                    category = aiCategoryEntity.get();
                    ticket.setCategory(category);
                }
            }
        }

        // 4. Apply rule-based deterministic routing
        if (category != null) {
            Optional<Team> targetTeam = routingService.resolveTargetTeam(category, request.priority());
            targetTeam.ifPresent(ticket::setAssignedTeam);
        }

        ticket = ticketRepository.save(ticket);

        // 4. Record initial TICKET_CREATED event
        TicketEvent createdEvent = new TicketEvent(
                ticket,
                customerUser,
                EventType.TICKET_CREATED,
                null,
                TicketStatus.OPEN.name(),
                "{\"ticketNumber\": \"" + ticketNumber + "\"}"
        );
        ticketEventRepository.save(createdEvent);

        // 5. If routed to a team, record assignment event
        if (ticket.getAssignedTeam() != null) {
            TicketAssignment assignment = new TicketAssignment(
                    ticket,
                    null,
                    ticket.getAssignedTeam(),
                    customerUser,
                    "Automated routing rule matched for category " + (category != null ? category.getName() : "N/A")
            );
            ticketAssignmentRepository.save(assignment);

            TicketEvent routingEvent = new TicketEvent(
                    ticket,
                    customerUser,
                    EventType.ASSIGNED,
                    "UNASSIGNED",
                    ticket.getAssignedTeam().getName(),
                    "{\"teamId\": \"" + ticket.getAssignedTeam().getId() + "\"}"
            );
            ticketEventRepository.save(routingEvent);
        }

        // 6. AI Sentiment Analysis (Phase 8.2)
        if (sentimentService != null) {
            sentimentService.analyzeAndPersistTicketSentiment(ticket, request.subject() + "\n" + request.description());
        }

        // 7. AI Duplicate Ticket Detection (Phase 8.3)
        if (duplicateService != null) {
            duplicateService.detectAndPersistDuplicates(ticket);
        }

        auditLogService.logAction(customerUser, "TICKET_CREATED", "Ticket", ticket.getId().toString(), null, ticket.getTicketNumber(), null, null);

        return EntityDtoMapper.toTicketResponse(ticket);
    }

    @Transactional(readOnly = true)
    public Ticket getTicketEntity(UUID ticketId) {
        return ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with ID: " + ticketId));
    }

    @Transactional(readOnly = true)
    public TicketDetailResponse getTicketDetails(UUID ticketId, User currentUser) {
        Ticket ticket = getTicketEntity(ticketId);
        assertCanAccessTicket(ticket, currentUser);

        boolean isStaff = currentUser.getRole() == UserRole.AGENT || currentUser.getRole() == UserRole.ADMIN;
        List<TicketMessage> messages = isStaff
                ? ticketMessageRepository.findByTicketIdOrderByCreatedAtAsc(ticket.getId())
                : ticketMessageRepository.findByTicketIdAndIsInternalFalseOrderByCreatedAtAsc(ticket.getId());

        List<TicketEvent> events = ticketEventRepository.findByTicketIdOrderByCreatedAtAsc(ticket.getId());

        var messageDtos = messages.stream().map(EntityDtoMapper::toMessageResponse).toList();
        var eventDtos = events.stream().map(e -> new TicketDetailResponse.TicketEventDto(
                e.getId(),
                e.getActor() != null && e.getActor().getProfile() != null
                        ? e.getActor().getProfile().getFirstName() + " " + e.getActor().getProfile().getLastName()
                        : "System",
                e.getEventType(),
                e.getOldValue(),
                e.getNewValue(),
                e.getMetadata(),
                e.getCreatedAt()
        )).toList();

        TicketSentimentAnalysis sentimentAnalysis = null;
        List<TicketDetailResponse.DuplicateMatchDto> duplicateMatches = null;
        if (isStaff) {
            if (sentimentService != null) {
                sentimentAnalysis = sentimentService.getLatestSentimentForTicket(ticket.getId()).orElse(null);
            }
            if (duplicateService != null) {
                duplicateMatches = duplicateService.getDuplicateMatchesForTicket(ticket.getId());
            }
        }

        return EntityDtoMapper.toTicketDetailResponse(ticket, messageDtos, eventDtos, sentimentAnalysis, duplicateMatches);
    }


    @Transactional(readOnly = true)
    public List<TicketSummaryResponse> getAllTickets() {
        return ticketRepository.findAll()
                .stream()
                .map(EntityDtoMapper::toTicketSummaryResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TicketSummaryResponse> getCustomerTickets(User customerUser) {
        Customer customer = customerRepository.findByUserId(customerUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer profile not found"));
        return ticketRepository.findByCustomerIdOrderByCreatedAtDesc(customer.getId())
                .stream()
                .map(EntityDtoMapper::toTicketSummaryResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TicketSummaryResponse> getAgentTickets(User agentUser) {
        Agent agent = agentRepository.findByUserId(agentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Agent profile not found"));
        return ticketRepository.findByAssignedAgentIdOrderByCreatedAtDesc(agent.getId())
                .stream()
                .map(EntityDtoMapper::toTicketSummaryResponse)
                .toList();
    }

    @Transactional
    public TicketResponse updateTicket(UUID ticketId, UpdateTicketRequest request, User currentUser) {
        Ticket ticket = getTicketEntity(ticketId);
        assertCanAccessTicket(ticket, currentUser);

        // Status transition validation
        if (request.status() != null && request.status() != ticket.getStatus()) {
            if (!ticket.getStatus().canTransitionTo(request.status())) {
                throw new BadRequestException("Invalid status transition from " + ticket.getStatus() + " to " + request.status());
            }

            TicketStatus oldStatus = ticket.getStatus();
            ticket.setStatus(request.status());

            if (request.status() == TicketStatus.RESOLVED) {
                ticket.setResolvedAt(OffsetDateTime.now());
            } else if (request.status() == TicketStatus.CLOSED) {
                ticket.setClosedAt(OffsetDateTime.now());
            }

            TicketEvent event = new TicketEvent(
                    ticket,
                    currentUser,
                    EventType.STATUS_CHANGED,
                    oldStatus.name(),
                    request.status().name(),
                    request.updateReason() != null ? "{\"reason\": \"" + request.updateReason() + "\"}" : null
            );
            ticketEventRepository.save(event);
        }

        // Priority change
        if (request.priority() != null && request.priority() != ticket.getPriority()) {
            TicketEvent event = new TicketEvent(
                    ticket,
                    currentUser,
                    EventType.PRIORITY_CHANGED,
                    ticket.getPriority().name(),
                    request.priority().name(),
                    null
            );
            ticketEventRepository.save(event);
            ticket.setPriority(request.priority());
        }

        // Category change
        if (request.categoryId() != null) {
            Category newCategory = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + request.categoryId()));
            String oldName = ticket.getCategory() != null ? ticket.getCategory().getName() : "NONE";
            ticket.setCategory(newCategory);
            TicketEvent event = new TicketEvent(ticket, currentUser, EventType.CATEGORY_CHANGED, oldName, newCategory.getName(), null);
            ticketEventRepository.save(event);
        }

        // Agent / Team assignment changes (Agents/Admins only)
        if (request.assignedAgentId() != null || request.assignedTeamId() != null) {
            if (currentUser.getRole() == UserRole.CUSTOMER) {
                throw new ForbiddenException("Customers cannot modify ticket assignments");
            }
            if (request.assignedAgentId() != null) {
                Agent agent = agentRepository.findById(request.assignedAgentId())
                        .orElseThrow(() -> new ResourceNotFoundException("Agent not found with ID: " + request.assignedAgentId()));
                ticket.setAssignedAgent(agent);
                if (agent.getTeam() != null) {
                    ticket.setAssignedTeam(agent.getTeam());
                }
            }
            if (request.assignedTeamId() != null) {
                Team team = teamRepository.findById(request.assignedTeamId())
                        .orElseThrow(() -> new ResourceNotFoundException("Team not found with ID: " + request.assignedTeamId()));
                ticket.setAssignedTeam(team);
            }

            TicketAssignment assignment = new TicketAssignment(
                    ticket,
                    ticket.getAssignedAgent(),
                    ticket.getAssignedTeam(),
                    currentUser,
                    request.updateReason() != null ? request.updateReason() : "Manual assignment update"
            );
            ticketAssignmentRepository.save(assignment);

            TicketEvent event = new TicketEvent(
                    ticket,
                    currentUser,
                    EventType.ASSIGNED,
                    null,
                    ticket.getAssignedAgent() != null ? ticket.getAssignedAgent().getEmployeeCode() : "TEAM_ASSIGNED",
                    null
            );
            ticketEventRepository.save(event);
        }

        ticket = ticketRepository.save(ticket);
        auditLogService.logAction(currentUser, "TICKET_UPDATED", "Ticket", ticket.getId().toString(), null, ticket.getStatus().name(), null, null);

        return EntityDtoMapper.toTicketResponse(ticket);
    }

    public void assertCanAccessTicket(Ticket ticket, User user) {
        if (user.getRole() == UserRole.ADMIN) {
            return;
        }
        if (user.getRole() == UserRole.CUSTOMER) {
            Customer customer = customerRepository.findByUserId(user.getId()).orElse(null);
            if (customer == null || !ticket.getCustomer().getId().equals(customer.getId())) {
                throw new ForbiddenException("You are not authorized to access this ticket");
            }
        }
        // Agents can access all tickets in their purview (assigned or team or open pool)
    }
}
