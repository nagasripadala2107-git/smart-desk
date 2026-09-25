package com.smartdesk.repository;

import com.smartdesk.entity.TicketDuplicateMatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TicketDuplicateMatchRepository extends JpaRepository<TicketDuplicateMatch, UUID> {

    @Query("SELECT m FROM TicketDuplicateMatch m " +
           "JOIN FETCH m.matchedTicket mt " +
           "LEFT JOIN FETCH mt.customer c " +
           "WHERE m.ticket.id = :ticketId " +
           "ORDER BY m.similarityScore DESC")
    List<TicketDuplicateMatch> findByTicketIdOrderBySimilarityScoreDesc(@Param("ticketId") UUID ticketId);

    @Query("SELECT m FROM TicketDuplicateMatch m " +
           "WHERE m.ticket.id = :ticketId AND m.matchedTicket.id = :matchedTicketId")
    Optional<TicketDuplicateMatch> findByTicketIdAndMatchedTicketId(@Param("ticketId") UUID ticketId,
                                                                   @Param("matchedTicketId") UUID matchedTicketId);

    @Query("SELECT COUNT(m) > 0 FROM TicketDuplicateMatch m " +
           "WHERE m.ticket.id = :ticketId AND m.matchedTicket.id = :matchedTicketId")
    boolean existsByTicketIdAndMatchedTicketId(@Param("ticketId") UUID ticketId,
                                              @Param("matchedTicketId") UUID matchedTicketId);
}
