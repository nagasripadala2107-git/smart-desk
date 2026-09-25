package com.smartdesk.service;

import com.smartdesk.entity.Category;
import com.smartdesk.entity.RoutingRule;
import com.smartdesk.entity.Team;
import com.smartdesk.entity.enums.TicketPriority;
import com.smartdesk.repository.RoutingRuleRepository;
import com.smartdesk.routing.TicketRoutingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketRoutingServiceTest {

    @Mock
    private RoutingRuleRepository routingRuleRepository;

    private TicketRoutingService routingService;

    @BeforeEach
    void setUp() {
        routingService = new TicketRoutingService(routingRuleRepository);
    }

    @Test
    void testResolveTargetTeam_ExactPriorityMatch() {
        Category category = new Category("BILLING", "Billing inquiries");
        category.setId(UUID.randomUUID());

        Team billingTeam = new Team("Billing Team", "Billing department");
        billingTeam.setId(UUID.randomUUID());

        RoutingRule rule = new RoutingRule("Billing Urgent Rule", category, TicketPriority.URGENT, billingTeam, 20);

        when(routingRuleRepository.findFirstByCategoryIdAndPriorityAndIsActiveTrueOrderByPriorityWeightDesc(category.getId(), TicketPriority.URGENT))
                .thenReturn(Optional.of(rule));

        Optional<Team> resolved = routingService.resolveTargetTeam(category, TicketPriority.URGENT);

        assertTrue(resolved.isPresent());
        assertEquals("Billing Team", resolved.get().getName());
    }

    @Test
    void testResolveTargetTeam_FallbackToCategoryDefault() {
        Category category = new Category("TECHNICAL", "Tech support");
        category.setId(UUID.randomUUID());

        Team techTeam = new Team("Technical Support", "Tech department");
        techTeam.setId(UUID.randomUUID());

        RoutingRule defaultRule = new RoutingRule("Technical Default Rule", category, null, techTeam, 5);

        when(routingRuleRepository.findFirstByCategoryIdAndPriorityAndIsActiveTrueOrderByPriorityWeightDesc(category.getId(), TicketPriority.LOW))
                .thenReturn(Optional.empty());
        when(routingRuleRepository.findFirstByCategoryIdAndPriorityIsNullAndIsActiveTrueOrderByPriorityWeightDesc(category.getId()))
                .thenReturn(Optional.of(defaultRule));

        Optional<Team> resolved = routingService.resolveTargetTeam(category, TicketPriority.LOW);

        assertTrue(resolved.isPresent());
        assertEquals("Technical Support", resolved.get().getName());
    }

    @Test
    void testResolveTargetTeam_NoRuleFound_ReturnsEmpty() {
        Category category = new Category("OTHER", "Other");
        category.setId(UUID.randomUUID());

        when(routingRuleRepository.findFirstByCategoryIdAndPriorityAndIsActiveTrueOrderByPriorityWeightDesc(category.getId(), TicketPriority.MEDIUM))
                .thenReturn(Optional.empty());
        when(routingRuleRepository.findFirstByCategoryIdAndPriorityIsNullAndIsActiveTrueOrderByPriorityWeightDesc(category.getId()))
                .thenReturn(Optional.empty());

        Optional<Team> resolved = routingService.resolveTargetTeam(category, TicketPriority.MEDIUM);

        assertTrue(resolved.isEmpty());
    }
}
