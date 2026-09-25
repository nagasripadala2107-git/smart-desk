package com.smartdesk.service;

import com.smartdesk.client.ai.ClassificationRequest;
import com.smartdesk.client.ai.ClassificationResponse;
import com.smartdesk.client.ai.TicketClassificationClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.ResourceAccessException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketClassificationServiceTest {

    @Mock
    private TicketClassificationClient classificationClient;

    private TicketClassificationService classificationService;

    @BeforeEach
    void setUp() {
        classificationService = new TicketClassificationService(classificationClient);
    }

    @Test
    void testClassifyTicket_Success() {
        ClassificationResponse mockResponse = new ClassificationResponse(
                "BILLING",
                0.9542,
                "ticket-classifier-v1"
        );
        when(classificationClient.classify(any(ClassificationRequest.class))).thenReturn(mockResponse);

        Optional<ClassificationResponse> result = classificationService.classifyTicket(
                "Payment issue",
                "I was charged twice on my invoice."
        );

        assertTrue(result.isPresent());
        assertEquals("BILLING", result.get().category());
        assertEquals(0.9542, result.get().confidence());
        assertEquals("ticket-classifier-v1", result.get().modelVersion());
    }

    @Test
    void testClassifyTicket_ClientThrowsTimeoutException_GracefulDegradation() {
        when(classificationClient.classify(any(ClassificationRequest.class)))
                .thenThrow(new ResourceAccessException("I/O error on POST request: Read timed out"));

        Optional<ClassificationResponse> result = classificationService.classifyTicket(
                "Slow request",
                "Testing timeout behavior"
        );

        // Must not throw, must return Optional.empty()
        assertTrue(result.isEmpty());
    }

    @Test
    void testClassifyTicket_ClientThrowsConnectionRefused_GracefulDegradation() {
        when(classificationClient.classify(any(ClassificationRequest.class)))
                .thenThrow(new ResourceAccessException("Connection refused: connect"));

        Optional<ClassificationResponse> result = classificationService.classifyTicket(
                "Offline service",
                "Testing offline behavior"
        );

        // Must not throw, must return Optional.empty()
        assertTrue(result.isEmpty());
    }

    @Test
    void testClassifyTicket_NullOrEmptyCategoryResponse_ReturnsEmptyOptional() {
        ClassificationResponse emptyCategoryResponse = new ClassificationResponse(
                "",
                0.0,
                "ticket-classifier-v1"
        );
        when(classificationClient.classify(any(ClassificationRequest.class))).thenReturn(emptyCategoryResponse);

        Optional<ClassificationResponse> result = classificationService.classifyTicket(
                "Test subject",
                "Test description"
        );

        assertTrue(result.isEmpty());
    }
}
