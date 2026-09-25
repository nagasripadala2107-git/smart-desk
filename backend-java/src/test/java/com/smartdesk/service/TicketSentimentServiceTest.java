package com.smartdesk.service;

import com.smartdesk.client.ai.SentimentRequest;
import com.smartdesk.client.ai.SentimentResponse;
import com.smartdesk.client.ai.TicketSentimentClient;
import com.smartdesk.entity.Customer;
import com.smartdesk.entity.Ticket;
import com.smartdesk.entity.TicketMessage;
import com.smartdesk.entity.TicketSentimentAnalysis;
import com.smartdesk.entity.User;
import com.smartdesk.entity.enums.CustomerTone;
import com.smartdesk.entity.enums.SentimentType;
import com.smartdesk.entity.enums.TicketPriority;
import com.smartdesk.entity.enums.UserRole;
import com.smartdesk.repository.TicketSentimentAnalysisRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.ResourceAccessException;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketSentimentServiceTest {

    @Mock
    private TicketSentimentClient sentimentClient;

    @Mock
    private TicketSentimentAnalysisRepository sentimentRepository;

    private TicketSentimentService sentimentService;

    private Ticket ticket;
    private User customerUser;
    private Customer customer;

    @BeforeEach
    void setUp() {
        sentimentService = new TicketSentimentService(sentimentClient, sentimentRepository);

        customerUser = new User("customer@test.com", "hash", UserRole.CUSTOMER);
        customer = new Customer(customerUser, "CUST-001", "Acme Inc", "STANDARD");
        ticket = new Ticket("SD-2026-000001", customer, null, "Billing Question", "Need invoice explanation", TicketPriority.MEDIUM);
        ticket.setId(UUID.randomUUID());
    }

    @Test
    @DisplayName("Should successfully analyze and persist ticket sentiment from AI client")
    void testAnalyzeAndPersistTicketSentimentSuccess() {
        SentimentResponse mockResponse = new SentimentResponse("POSITIVE", 0.9450, "SATISFIED", "ticket-sentiment-v1");
        when(sentimentClient.analyze(any(SentimentRequest.class))).thenReturn(mockResponse);
        when(sentimentRepository.save(any(TicketSentimentAnalysis.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Optional<TicketSentimentAnalysis> result = sentimentService.analyzeAndPersistTicketSentiment(
                ticket, "Thank you so much! Everything is working great."
        );

        assertTrue(result.isPresent());
        TicketSentimentAnalysis analysis = result.get();
        assertEquals(SentimentType.POSITIVE, analysis.getSentiment());
        assertEquals(CustomerTone.SATISFIED, analysis.getTone());
        assertEquals(new BigDecimal("0.9450"), analysis.getConfidence());
        assertEquals("ticket-sentiment-v1", analysis.getModelVersion());
        assertNotNull(analysis.getAnalyzedTextHash());
        assertNull(analysis.getMessage());
        assertEquals(ticket, analysis.getTicket());

        verify(sentimentRepository, times(1)).save(any(TicketSentimentAnalysis.class));
    }

    @Test
    @DisplayName("Should successfully analyze message sentiment when message belongs to same ticket")
    void testAnalyzeAndPersistMessageSentimentRelationshipIntegritySuccess() {
        TicketMessage message = new TicketMessage(ticket, customerUser, "Please help me with this charge", false);
        message.setId(UUID.randomUUID());

        SentimentResponse mockResponse = new SentimentResponse("NEGATIVE", 0.8800, "FRUSTRATED", "ticket-sentiment-v1");
        when(sentimentClient.analyze(any(SentimentRequest.class))).thenReturn(mockResponse);
        when(sentimentRepository.save(any(TicketSentimentAnalysis.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Optional<TicketSentimentAnalysis> result = sentimentService.analyzeAndPersistMessageSentiment(
                ticket, message, "Please help me with this charge"
        );

        assertTrue(result.isPresent());
        TicketSentimentAnalysis analysis = result.get();
        assertEquals(ticket, analysis.getTicket());
        assertEquals(message, analysis.getMessage());
        assertEquals(SentimentType.NEGATIVE, analysis.getSentiment());
        assertEquals(CustomerTone.FRUSTRATED, analysis.getTone());
    }

    @Test
    @DisplayName("Should reject sentiment persistence with IllegalArgumentException if message belongs to different ticket")
    void testRejectMismatchedTicketMessageRelationship() {
        Ticket otherTicket = new Ticket("SD-2026-000002", customer, null, "Other", "Desc", TicketPriority.LOW);
        otherTicket.setId(UUID.randomUUID());

        TicketMessage messageBelongingToOtherTicket = new TicketMessage(otherTicket, customerUser, "Message text", false);
        messageBelongingToOtherTicket.setId(UUID.randomUUID());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            sentimentService.analyzeAndPersistMessageSentiment(
                    ticket, messageBelongingToOtherTicket, "Message text"
            );
        });

        assertTrue(ex.getMessage().contains("Relationship integrity violation"));
        verifyNoInteractions(sentimentClient);
        verifyNoInteractions(sentimentRepository);
    }

    @Test
    @DisplayName("Should reject sentiment persistence with IllegalArgumentException if message ticket is null")
    void testRejectNullTicketOnMessage() {
        TicketMessage orphanMessage = new TicketMessage();
        orphanMessage.setId(UUID.randomUUID());
        orphanMessage.setMessage("Orphan message text");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            sentimentService.analyzeAndPersistMessageSentiment(
                    ticket, orphanMessage, "Orphan message text"
            );
        });

        assertTrue(ex.getMessage().contains("Relationship integrity violation"));
        verifyNoInteractions(sentimentClient);
        verifyNoInteractions(sentimentRepository);
    }

    @Test
    @DisplayName("Should degrade gracefully and return empty Optional when AI client throws exception")
    void testGracefulDegradationOnAiClientError() {
        when(sentimentClient.analyze(any(SentimentRequest.class)))
                .thenThrow(new ResourceAccessException("Connection refused"));

        Optional<TicketSentimentAnalysis> result = assertDoesNotThrow(() ->
                sentimentService.analyzeAndPersistTicketSentiment(ticket, "Test inquiry text")
        );

        assertTrue(result.isEmpty());
        verify(sentimentRepository, never()).save(any(TicketSentimentAnalysis.class));
    }

    @Test
    @DisplayName("Should return empty Optional when input text is blank")
    void testBlankTextReturnsEmpty() {
        Optional<TicketSentimentAnalysis> result1 = sentimentService.analyzeAndPersistTicketSentiment(ticket, "");
        Optional<TicketSentimentAnalysis> result2 = sentimentService.analyzeAndPersistTicketSentiment(ticket, "   ");

        assertTrue(result1.isEmpty());
        assertTrue(result2.isEmpty());
        verifyNoInteractions(sentimentClient);
    }

    @Test
    @DisplayName("Should fallback to NEUTRAL when client returns unknown sentiment or tone")
    void testUnknownSentimentFallback() {
        SentimentResponse mockResponse = new SentimentResponse("UNKNOWN_VAL", 0.5000, "WEIRD_TONE", "ticket-sentiment-v1");
        when(sentimentClient.analyze(any(SentimentRequest.class))).thenReturn(mockResponse);
        when(sentimentRepository.save(any(TicketSentimentAnalysis.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Optional<TicketSentimentAnalysis> result = sentimentService.analyzeAndPersistTicketSentiment(
                ticket, "Neutral inquiry"
        );

        assertTrue(result.isPresent());
        assertEquals(SentimentType.NEUTRAL, result.get().getSentiment());
        assertEquals(CustomerTone.NEUTRAL, result.get().getTone());
    }
}
