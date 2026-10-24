package dev.bankinsights.backend;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class AstropodsClient {

    private final RestClient client;
    private final String model;

    public AstropodsClient(
        @Value("${bankinsights.ai.gateway-url}") String gatewayUrl,
        @Value("${bankinsights.ai.api-key}") String apiKey,
        @Value("${bankinsights.ai.model}") String model
    ) {
        this.model = model;

        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(5_000);
        requestFactory.setReadTimeout(30_000);

        this.client = RestClient.builder()
            .baseUrl(gatewayUrl)
            .requestFactory(requestFactory)
            .defaultHeaders(headers -> headers.setBearerAuth(apiKey))
            .build();
    }

    public String ask(String instructions, String input) {
        var request = Map.of(
            "model", model,
            "messages", List.of(
                Map.of("role", "system", "content", instructions),
                Map.of("role", "user", "content", input)
            )
        );

        return client.post()
            .uri("/v1/chat/completions")
            .contentType(MediaType.APPLICATION_JSON)
            .body(request)
            .retrieve()
            .body(String.class);
    }
}