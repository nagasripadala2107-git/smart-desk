package com.smartdesk.repository;

import com.smartdesk.entity.Escalation;
import com.smartdesk.entity.enums.EscalationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EscalationRepository extends JpaRepository<Escalation, UUID> {
    List<Escalation> findByTicketIdOrderByCreatedAtDesc(UUID ticketId);
    List<Escalation> findByStatusOrderByCreatedAtDesc(EscalationStatus status);

    @org.springframework.data.jpa.repository.Query("SELECT e.level, COUNT(e) FROM Escalation e GROUP BY e.level")
    List<Object[]> countGroupByLevel();

    @org.springframework.data.jpa.repository.Query("SELECT e.toTeam.name, COUNT(e) FROM Escalation e GROUP BY e.toTeam.name")
    List<Object[]> countGroupByToTeam();

    List<Escalation> findTop10ByOrderByCreatedAtDesc();
}
