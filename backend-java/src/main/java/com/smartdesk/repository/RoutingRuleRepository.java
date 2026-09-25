package com.smartdesk.repository;

import com.smartdesk.entity.RoutingRule;
import com.smartdesk.entity.enums.TicketPriority;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoutingRuleRepository extends JpaRepository<RoutingRule, UUID> {
    List<RoutingRule> findByCategoryIdAndIsActiveTrueOrderByPriorityWeightDesc(UUID categoryId);
    Optional<RoutingRule> findFirstByCategoryIdAndPriorityAndIsActiveTrueOrderByPriorityWeightDesc(UUID categoryId, TicketPriority priority);
    Optional<RoutingRule> findFirstByCategoryIdAndPriorityIsNullAndIsActiveTrueOrderByPriorityWeightDesc(UUID categoryId);
}
