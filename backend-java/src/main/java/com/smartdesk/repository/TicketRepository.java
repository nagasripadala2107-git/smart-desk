package com.smartdesk.repository;

import com.smartdesk.entity.Ticket;
import com.smartdesk.entity.enums.TicketPriority;
import com.smartdesk.entity.enums.TicketStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, UUID> {

    Optional<Ticket> findByTicketNumber(String ticketNumber);

    List<Ticket> findByCustomerIdOrderByCreatedAtDesc(UUID customerId);

    List<Ticket> findByAssignedAgentIdOrderByCreatedAtDesc(UUID agentId);

    List<Ticket> findByAssignedTeamIdOrderByCreatedAtDesc(UUID teamId);

    List<Ticket> findByStatusOrderByCreatedAtDesc(TicketStatus status);

    List<Ticket> findByPriorityOrderByCreatedAtDesc(TicketPriority priority);

    List<Ticket> findByCategoryIdOrderByCreatedAtDesc(UUID categoryId);

    long countByStatus(TicketStatus status);

    long countByPriority(TicketPriority priority);

    long countByCategoryId(UUID categoryId);

    long countByAssignedAgentIdAndStatusIn(UUID agentId, Collection<TicketStatus> statuses);

    @Query("SELECT t.ticketNumber FROM Ticket t WHERE t.ticketNumber LIKE :prefix% ORDER BY t.ticketNumber DESC LIMIT 1")
    Optional<String> findLatestTicketNumberWithPrefix(@Param("prefix") String prefix);

    @Query("SELECT t.status, COUNT(t) FROM Ticket t GROUP BY t.status")
    List<Object[]> countGroupByStatus();

    @Query("SELECT t.priority, COUNT(t) FROM Ticket t GROUP BY t.priority")
    List<Object[]> countGroupByPriority();

    @Query("SELECT c.id, c.name, COUNT(t) FROM Ticket t JOIN t.category c GROUP BY c.id, c.name")
    List<Object[]> countGroupByAssignedCategory();

    @Query("SELECT CAST(t.createdAt AS LocalDate), COUNT(t) FROM Ticket t WHERE t.createdAt >= :startDate GROUP BY CAST(t.createdAt AS LocalDate) ORDER BY CAST(t.createdAt AS LocalDate) ASC")
    List<Object[]> countTicketsByDaySince(@Param("startDate") java.time.OffsetDateTime startDate);

    @Query("SELECT CAST(t.createdAt AS LocalDate), COUNT(t) FROM Ticket t GROUP BY CAST(t.createdAt AS LocalDate) ORDER BY CAST(t.createdAt AS LocalDate) ASC")
    List<Object[]> countTicketsByDayAllTime();

    @Query("SELECT t.createdAt, t.resolvedAt FROM Ticket t WHERE t.resolvedAt IS NOT NULL")
    List<Object[]> findResolutionTimestamps();

    @Query("SELECT t.assignedTeam.id, t.status, COUNT(t) FROM Ticket t WHERE t.assignedTeam IS NOT NULL GROUP BY t.assignedTeam.id, t.status")
    List<Object[]> countTicketsByTeamAndStatus();

    @Query("SELECT t.assignedAgent.id, t.status, COUNT(t) FROM Ticket t WHERE t.assignedAgent IS NOT NULL GROUP BY t.assignedAgent.id, t.status")
    List<Object[]> countTicketsByAgentAndStatus();

    @Query("SELECT t.assignedAgent.id, t.createdAt, t.resolvedAt FROM Ticket t WHERE t.assignedAgent IS NOT NULL AND t.resolvedAt IS NOT NULL")
    List<Object[]> findAgentResolutionTimestamps();

    @Query("SELECT t.priority, t.createdAt FROM Ticket t WHERE t.status NOT IN (com.smartdesk.entity.enums.TicketStatus.RESOLVED, com.smartdesk.entity.enums.TicketStatus.CLOSED)")
    List<Object[]> findActiveTicketsForSla();

    @Query("SELECT t FROM Ticket t WHERE t.id <> :excludeTicketId ORDER BY t.createdAt DESC")
    List<Ticket> findCandidatesForDuplicateDetection(@Param("excludeTicketId") UUID excludeTicketId, Pageable pageable);
}
