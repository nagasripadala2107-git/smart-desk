package com.smartdesk.repository;

import com.smartdesk.entity.Agent;
import com.smartdesk.entity.enums.AgentAvailability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AgentRepository extends JpaRepository<Agent, UUID> {
    Optional<Agent> findByUserId(UUID userId);
    Optional<Agent> findByEmployeeCode(String employeeCode);
    List<Agent> findByTeamIdAndAvailabilityStatus(UUID teamId, AgentAvailability availabilityStatus);
    List<Agent> findByAvailabilityStatus(AgentAvailability status);
}
