package com.smartdesk.routing;

import com.smartdesk.entity.Category;
import com.smartdesk.entity.RoutingRule;
import com.smartdesk.entity.Team;
import com.smartdesk.entity.enums.TicketPriority;
import com.smartdesk.repository.RoutingRuleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class TicketRoutingService {

    private static final Logger log = LoggerFactory.getLogger(TicketRoutingService.class);
    private final RoutingRuleRepository routingRuleRepository;

    public TicketRoutingService(RoutingRuleRepository routingRuleRepository) {
        this.routingRuleRepository = routingRuleRepository;
    }

    /**
     * Resolves the target team based on category and priority routing rules.
     * Deterministic rule-based matching:
     * 1. Check for specific (category, priority) match.
     * 2. Fall back to category-level default match (priority is null).
     *
     * Note: In Phase 5, this will be preceded by the Python AI classification service.
     */
    public Optional<Team> resolveTargetTeam(Category category, TicketPriority priority) {
        if (category == null) {
            return Optional.empty();
        }

        // 1. Try exact match on category and priority
        if (priority != null) {
            Optional<RoutingRule> specificRule = routingRuleRepository
                    .findFirstByCategoryIdAndPriorityAndIsActiveTrueOrderByPriorityWeightDesc(category.getId(), priority);
            if (specificRule.isPresent()) {
                log.info("Matched specific routing rule '{}' for category={} and priority={}",
                        specificRule.get().getName(), category.getName(), priority);
                return Optional.of(specificRule.get().getTeam());
            }
        }

        // 2. Fall back to category default rule
        Optional<RoutingRule> defaultRule = routingRuleRepository
                .findFirstByCategoryIdAndPriorityIsNullAndIsActiveTrueOrderByPriorityWeightDesc(category.getId());
        if (defaultRule.isPresent()) {
            log.info("Matched default routing rule '{}' for category={}",
                    defaultRule.get().getName(), category.getName());
            return Optional.of(defaultRule.get().getTeam());
        }

        log.warn("No active routing rule found for category={}", category.getName());
        return Optional.empty();
    }
}
