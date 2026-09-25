package com.smartdesk.client.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Component
public class TicketSentimentClient {

    private final RestClient restClient;

    @org.springframework.beans.factory.annotation.Autowired
    public TicketSentimentClient(
            @Value("${app.ai.service-url:http://localhost:8000}") String serviceUrl,
            @Value("${app.ai.timeout-ms:2000}") int timeoutMs
    ) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(timeoutMs));
        requestFactory.setReadTimeout(Duration.ofMillis(timeoutMs));

        String normalizedUrl = (serviceUrl.startsWith("http://") || serviceUrl.startsWith("https://"))
                ? serviceUrl
                : "http://" + serviceUrl;

        this.restClient = RestClient.builder()
                .baseUrl(normalizedUrl)
                .requestFactory(requestFactory)
                .build();
    }

    public TicketSentimentClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public SentimentResponse analyze(SentimentRequest request) {
        return restClient.post()
                .uri("/api/v1/sentiment/analyze")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(SentimentResponse.class);
    }
}
