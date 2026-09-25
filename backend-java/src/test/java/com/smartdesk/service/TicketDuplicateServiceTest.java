package com.smartdesk.service;

import com.smartdesk.client.ai.*;
import com.smartdesk.dto.ticket.TicketDetailResponse;
import com.smartdesk.entity.Customer;
import com.smartdesk.entity.Ticket;
import com.smartdesk.entity.TicketDuplicateMatch;
import com.smartdesk.entity.User;
import com.smartdesk.entity.enums.TicketPriority;
import com.smartdesk.entity.enums.UserRole;
import com.smartdesk.repository.TicketDuplicateMatchRepository;
import com.smartdesk.repository.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.web.client.ResourceAccessException;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketDuplicateServiceTest {

    @Mock
    private TicketDuplicateClient duplicateClient;

    @Mock
    private TicketDuplicateMatchRepository duplicateMatchRepository;

    @Mock
    private TicketRepository ticketRepository;

    private TicketDuplicateService duplicateService;

    private Ticket targetTicket;
    private Ticket candidateTicket1;
    private Ticket candidateTicket2;
    private Customer customer;

    @BeforeEach
    void setUp() {
        duplicateService = new TicketDuplicateService(duplicateClient, duplicateMatchRepository, ticketRepository);
        duplicateService.setSimilarityThreshold(0.70);
        duplicateService.setMaxMatches(5);
        duplicateService.setMaxCandidates(50);

        User customerUser = new User("customer@test.com", "hash", UserRole.CUSTOMER);
        customer = new Customer(customerUser, "CUST-001", "Acme Inc", "STANDARD");

        targetTicket = new Ticket("SD-2026-000001", customer, null, "Double charge on card", "I was billed twice", TicketPriority.HIGH);
        targetTicket.setId(UUID.randomUUID());

        candidateTicket1 = new Ticket("SD-2026-000002", customer, null, "Payment billed two times", "Charged two times for one order", TicketPriority.HIGH);
        candidateTicket1.setId(UUID.randomUUID());

        candidateTicket2 = new Ticket("SD-2026-000003", customer, null, "Password reset", "Cannot reset password", TicketPriority.LOW);
        candidateTicket2.setId(UUID.randomUUID());
    }

    @Test
    @DisplayName("Should detect and persist potential duplicate tickets above threshold")
    void testDetectAndPersistDuplicatesSuccess() {
        when(ticketRepository.findCandidatesForDuplicateDetection(eq(targetTicket.getId()), any(Pageable.class)))
                .thenReturn(List.of(candidateTicket1, candidateTicket2));

        DuplicateDetectionResponse mockResponse = new DuplicateDetectionResponse(
                List.of(
                        new DuplicateMatchItemDto(candidateTicket1.getId().toString(), 0.88),
                        new DuplicateMatchItemDto(candidateTicket2.getId().toString(), 0.15)
                ),
                "ticket-duplicate-v1"
        );

        when(duplicateClient.analyze(any(DuplicateDetectionRequest.class))).thenReturn(mockResponse);
        when(duplicateMatchRepository.existsByTicketIdAndMatchedTicketId(any(), any())).thenReturn(false);
        when(duplicateMatchRepository.save(any(TicketDuplicateMatch.class))).thenAnswer(i -> i.getArgument(0));

        List<TicketDuplicateMatch> matches = duplicateService.detectAndPersistDuplicates(targetTicket);

        assertEquals(1, matches.size());
        TicketDuplicateMatch match = matches.get(0);
        assertEquals(targetTicket, match.getTicket());
        assertEquals(candidateTicket1, match.getMatchedTicket());
        assertEquals(new BigDecimal("0.8800"), match.getSimilarityScore());
        assertEquals("ticket-duplicate-v1", match.getModelVersion());

        verify(duplicateMatchRepository, times(1)).save(any(TicketDuplicateMatch.class));
    }

    @Test
    @DisplayName("Should strictly reject and filter out self-matches")
    void testSelfMatchRejectedAtApplicationLayer() {
        when(ticketRepository.findCandidatesForDuplicateDetection(eq(targetTicket.getId()), any(Pageable.class)))
                .thenReturn(List.of(targetTicket, candidateTicket1));

        DuplicateDetectionResponse mockResponse = new DuplicateDetectionResponse(
                List.of(
                        new DuplicateMatchItemDto(targetTicket.getId().toString(), 1.00),
                        new DuplicateMatchItemDto(candidateTicket1.getId().toString(), 0.85)
                ),
                "ticket-duplicate-v1"
        );

        when(duplicateClient.analyze(any(DuplicateDetectionRequest.class))).thenReturn(mockResponse);
        when(duplicateMatchRepository.existsByTicketIdAndMatchedTicketId(any(), any())).thenReturn(false);
        when(duplicateMatchRepository.save(any(TicketDuplicateMatch.class))).thenAnswer(i -> i.getArgument(0));

        List<TicketDuplicateMatch> matches = duplicateService.detectAndPersistDuplicates(targetTicket);

        // Target ticket itself must NEVER be saved as a match
        assertEquals(1, matches.size());
        assertEquals(candidateTicket1, matches.get(0).getMatchedTicket());
        verify(duplicateMatchRepository, times(1)).save(any(TicketDuplicateMatch.class));
    }

    @Test
    @DisplayName("TicketDuplicateMatch entity constructor should throw IllegalArgumentException on self-match")
    void testEntityRejectsSelfMatch() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            new TicketDuplicateMatch(targetTicket, targetTicket, new BigDecimal("1.0000"), "ticket-duplicate-v1");
        });
        assertTrue(ex.getMessage().contains("Self-match violation"));
    }

    @Test
    @DisplayName("Should gracefully degrade and return empty list when AI service throws exception")
    void testGracefulDegradationOnAiFailure() {
        when(ticketRepository.findCandidatesForDuplicateDetection(eq(targetTicket.getId()), any(Pageable.class)))
                .thenReturn(List.of(candidateTicket1));

        when(duplicateClient.analyze(any(DuplicateDetectionRequest.class)))
                .thenThrow(new ResourceAccessException("Connection refused: AI service down"));

        List<TicketDuplicateMatch> matches = assertDoesNotThrow(() ->
                duplicateService.detectAndPersistDuplicates(targetTicket)
        );

        assertTrue(matches.isEmpty());
        verify(duplicateMatchRepository, never()).save(any(TicketDuplicateMatch.class));
    }

    @Test
    @DisplayName("Should return empty list without calling AI client when no candidates exist")
    void testEmptyCandidatesReturnsEmptyWithoutCallingClient() {
        when(ticketRepository.findCandidatesForDuplicateDetection(eq(targetTicket.getId()), any(Pageable.class)))
                .thenReturn(Collections.emptyList());

        List<TicketDuplicateMatch> matches = duplicateService.detectAndPersistDuplicates(targetTicket);

        assertTrue(matches.isEmpty());
        verifyNoInteractions(duplicateClient);
        verifyNoInteractions(duplicateMatchRepository);
    }

    @Test
    @DisplayName("Should retrieve duplicate matches for agent details view")
    void testGetDuplicateMatchesForTicket() {
        TicketDuplicateMatch match = new TicketDuplicateMatch(
                targetTicket, candidateTicket1, new BigDecimal("0.8750"), "ticket-duplicate-v1"
        );

        when(duplicateMatchRepository.findByTicketIdOrderBySimilarityScoreDesc(targetTicket.getId()))
                .thenReturn(List.of(match));

        List<TicketDetailResponse.DuplicateMatchDto> dtos = duplicateService.getDuplicateMatchesForTicket(targetTicket.getId());

        assertEquals(1, dtos.size());
        assertEquals(candidateTicket1.getId(), dtos.get(0).ticketId());
        assertEquals(candidateTicket1.getTicketNumber(), dtos.get(0).ticketNumber());
        assertEquals(candidateTicket1.getSubject(), dtos.get(0).subject());
        assertEquals(new BigDecimal("0.8750"), dtos.get(0).similarityScore());
        assertEquals("ticket-duplicate-v1", dtos.get(0).modelVersion());
    }
}
