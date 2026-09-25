package com.smartdesk.service;

import com.smartdesk.client.ai.ClassificationRequest;
import com.smartdesk.client.ai.ClassificationResponse;
import com.smartdesk.client.ai.TicketClassificationClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class TicketClassificationService {

    private static final Logger log = LoggerFactory.getLogger(TicketClassificationService.class);

    private final TicketClassificationClient classificationClient;

    public TicketClassificationService(TicketClassificationClient classificationClient) {
        this.classificationClient = classificationClient;
    }

    /**
     * Classifies a ticket asynchronously/synchronously via the Python AI microservice.
     * Guaranteed never to throw an exception to callers (graceful degradation).
     *
     * @param subject the ticket subject
     * @param description the ticket description
     * @return Optional containing ClassificationResponse if successful, empty if failed or offline
     */
    public Optional<ClassificationResponse> classifyTicket(String subject, String description) {
        try {
            ClassificationRequest request = new ClassificationRequest(subject, description);
            ClassificationResponse response = classificationClient.classify(request);

            if (response != null && response.category() != null && !response.category().isBlank()) {
                log.info("AI classification successful: category={}, confidence={}, model_version={}",
                        response.category(), response.confidence(), response.modelVersion());
                return Optional.of(response);
            }

            log.warn("AI classification returned an empty or invalid category response. Falling back to deterministic routing.");
            return Optional.empty();
        } catch (Exception ex) {
            // Log concise warning without logging raw sensitive customer ticket contents
            log.warn("AI classification microservice unavailable or timed out ({}). Continuing with fallback routing.",
                    ex.getClass().getSimpleName() + ": " + ex.getMessage());
            return Optional.empty();
        }
    }
}
