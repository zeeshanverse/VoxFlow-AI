package com.voxflow.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Primary
@Service
public class GeminiLlmProvider implements LlmProvider {

    private static final int MAX_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 1500;

    private final WebClient client;
    private final ObjectMapper mapper = new ObjectMapper();

    private final String key;
    private final String model;
    private final String baseUrl;

    public GeminiLlmProvider(
            WebClient client,
            @Value("${gemini.api-key:}") String key,
            @Value("${gemini.model:gemini-3.8-flash}") String model,
            @Value("${gemini.base-url:https://generativelanguage.googleapis.com/v1beta}") String baseUrl
    ) {
        this.client = client;
        this.key = key;
        this.model = model;
        this.baseUrl = baseUrl;
    }

    @Override
    public String generate(String instructions, String input) {

        if (key == null || key.isBlank()) {
            return "LLM is not configured yet. Configure GEMINI_API_KEY.";
        }

        WebClientResponseException lastError = null;

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {

            try {
                return callGemini(instructions, input);

            } catch (WebClientResponseException e) {

                lastError = e;

                boolean overloaded = e.getStatusCode().value() == 503;

                System.err.println(
                        "Gemini HTTP " + e.getStatusCode().value()
                                + " (attempt " + attempt + "/" + MAX_ATTEMPTS + "): "
                                + e.getResponseBodyAsString()
                );

                if (!overloaded || attempt == MAX_ATTEMPTS) {
                    throw new IllegalStateException(
                            "Gemini request failed with HTTP "
                                    + e.getStatusCode().value()
                                    + ": "
                                    + e.getResponseBodyAsString()
                    );
                }

                try {
                    Thread.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Gemini retry interrupted", ie);
                }

            } catch (Exception e) {

                e.printStackTrace();

                throw new IllegalStateException(
                        "LLM request failed: " + e.getMessage(),
                        e
                );
            }
        }

        throw new IllegalStateException(
                "Gemini request failed after " + MAX_ATTEMPTS + " attempts",
                lastError
        );
    }

    private String callGemini(String instructions, String input) {

        String uri = baseUrl + "/models/" + model + ":generateContent?key=" + key;

        Map<String, Object> body = Map.of(
                "systemInstruction", Map.of(
                        "parts", List.of(Map.of("text", instructions))
                ),
                "contents", List.of(
                        Map.of(
                                "role", "user",
                                "parts", List.of(Map.of("text", input))
                        )
                )
        );

        String raw = client.post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .block(Duration.ofSeconds(90));

        JsonNode root;
        try {
            root = mapper.readTree(raw);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Gemini response", e);
        }

        JsonNode candidates = root.path("candidates");

        StringBuilder text = new StringBuilder();

        if (candidates.isArray() && candidates.size() > 0) {

            JsonNode parts = candidates.get(0).path("content").path("parts");

            for (JsonNode part : parts) {
                if (part.has("text")) {
                    text.append(part.get("text").asText());
                }
            }
        }

        if (!text.isEmpty()) {
            return text.toString();
        }

        throw new RuntimeException("Gemini returned no text output");
    }
}