package com.smartdesk.repository;

import com.smartdesk.entity.SlaPolicy;
import com.smartdesk.entity.enums.TicketPriority;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SlaPolicyRepository extends JpaRepository<SlaPolicy, UUID> {
    Optional<SlaPolicy> findByPriority(TicketPriority priority);
}
