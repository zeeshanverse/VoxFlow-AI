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

    private static final int MAX_ATTEMPTS = 2;
    private static final long RETRY_DELAY_MS = 1000;
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

    private final WebClient client;
    private final ObjectMapper mapper = new ObjectMapper();

    private final String key;
    private final String model;
    private final String baseUrl;
    private final String thinkingLevel;

    public GeminiLlmProvider(
            WebClient client,
            @Value("${gemini.api-key:}") String key,
            @Value("${gemini.model:gemini-3.8-flash}") String model,
            @Value("${gemini.base-url:https://generativelanguage.googleapis.com/v1beta}") String baseUrl,
            @Value("${gemini.thinking-level:low}") String thinkingLevel
    ) {
        this.client = client;
        this.key = key;
        this.model = model;
        this.baseUrl = baseUrl;
        this.thinkingLevel = thinkingLevel;
    }

    @Override
    public String generate(String instructions, String input) {
        if (key == null || key.isBlank()) {
            throw new IllegalStateException("Gemini is not configured: GEMINI_API_KEY is missing");
        }

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            long started = System.currentTimeMillis();

            try {
                String result = callGemini(instructions, input);
                System.out.println("Gemini completed in " + (System.currentTimeMillis() - started) + " ms");
                return result;
            } catch (WebClientResponseException e) {
                int status = e.getStatusCode().value();
                System.err.println(
                        "Gemini HTTP " + status + " on attempt " + attempt + "/" + MAX_ATTEMPTS
                );
                System.err.println("Gemini response body: " + e.getResponseBodyAsString());

                boolean retryable = status == 429 || status == 503;
                if (!retryable || attempt == MAX_ATTEMPTS) {
                    throw new IllegalStateException(
                            "Gemini request failed with HTTP " + status
                    );
                }

                sleepBeforeRetry();
            } catch (IllegalStateException e) {
                throw e;
            } catch (Exception e) {
                System.err.println("Gemini request error: " + e.getMessage());
                throw new IllegalStateException("Gemini request failed");
            }
        }

        throw new IllegalStateException("Gemini request failed");
    }

    private String callGemini(String instructions, String input) {
        String uri = baseUrl + "/models/" + model + ":generateContent";

        Map<String, Object> body = Map.of(
                "systemInstruction", Map.of(
                        "parts", List.of(Map.of("text", instructions))
                ),
                "contents", List.of(
                        Map.of(
                                "role", "user",
                                "parts", List.of(Map.of("text", input))
                        )
                ),
                "generationConfig", Map.of(
                        "thinkingConfig", Map.of(
                                "thinkingLevel", thinkingLevel
                        )
                )
        );

        String raw = client.post()
                .uri(uri)
                .header("x-goog-api-key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .block(REQUEST_TIMEOUT);

        try {
            JsonNode root = mapper.readTree(raw);
            JsonNode candidates = root.path("candidates");

            if (candidates.isArray() && !candidates.isEmpty()) {
                StringBuilder text = new StringBuilder();
                for (JsonNode part : candidates.get(0).path("content").path("parts")) {
                    if (part.has("text")) {
                        text.append(part.get("text").asText());
                    }
                }
                if (!text.isEmpty()) return text.toString();
            }
        } catch (Exception e) {
            throw new IllegalStateException("Gemini returned an invalid response");
        }

        throw new IllegalStateException("Gemini returned no text output");
    }

    private void sleepBeforeRetry() {
        try {
            Thread.sleep(RETRY_DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Gemini retry interrupted");
        }
    }
}
