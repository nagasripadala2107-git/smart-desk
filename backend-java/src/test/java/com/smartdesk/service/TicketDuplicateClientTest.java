package com.smartdesk.service;

import com.smartdesk.client.ai.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class TicketDuplicateClientTest {

    @Test
    @DisplayName("Should post to /api/v1/duplicates/analyze and parse response")
    void testClientAnalyzeSuccess() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://localhost:8000");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        TicketDuplicateClient client = new TicketDuplicateClient(builder.build());

        String jsonResponse = """
                {
                  "matches": [
                    {
                      "ticketId": "SD-123",
                      "similarity": 0.885
                    }
                  ],
                  "modelVersion": "ticket-duplicate-v1"
                }
                """;

        server.expect(requestTo("http://localhost:8000/api/v1/duplicates/analyze"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        DuplicateDetectionRequest request = new DuplicateDetectionRequest(
                "Charged twice",
                List.of(new DuplicateCandidateDto("SD-123", "Billed twice"))
        );

        DuplicateDetectionResponse response = client.analyze(request);

        assertNotNull(response);
        assertEquals("ticket-duplicate-v1", response.modelVersion());
        assertEquals(1, response.matches().size());
        assertEquals("SD-123", response.matches().get(0).ticketId());
        assertEquals(0.885, response.matches().get(0).similarity());
        server.verify();
    }
}
