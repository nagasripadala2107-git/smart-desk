package com.smartdesk.client.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Component
public class TicketClassificationClient {

    private final RestClient restClient;

    @org.springframework.beans.factory.annotation.Autowired
    public TicketClassificationClient(
            @Value("${app.ai.service-url:http://localhost:8000}") String serviceUrl,
            @Value("${app.ai.timeout-ms:2000}") int timeoutMs
    ) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(timeoutMs));
        requestFactory.setReadTimeout(Duration.ofMillis(timeoutMs));

        this.restClient = RestClient.builder()
                .baseUrl(serviceUrl)
                .requestFactory(requestFactory)
                .build();
    }

    public TicketClassificationClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public ClassificationResponse classify(ClassificationRequest request) {
        return restClient.post()
                .uri("/api/v1/classify")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(ClassificationResponse.class);
    }
}
