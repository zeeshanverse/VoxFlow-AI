package com.voxflow.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.Map;

@Service
public class OpenAiLlmProvider implements LlmProvider {

    private final WebClient client;
    private final ObjectMapper mapper = new ObjectMapper();

    private final String key;
    private final String model;
    private final String baseUrl;

    public OpenAiLlmProvider(
            WebClient client,
            @Value("${openai.api-key:}") String key,
            @Value("${openai.model:gpt-6-astra}") String model,
            @Value("${openai.base-url:https://api.openai.com/v1}") String baseUrl
    ) {
        this.client = client;
        this.key = key;
        this.model = model;
        this.baseUrl = baseUrl;
    }

    @Override
    public String generate(String instructions, String input) {

        if (key == null || key.isBlank()) {
            return "LLM is not configured yet. Configure OPENAI_API_KEY.";
        }

        try {

            String raw = client.post()
                    .uri(baseUrl + "/responses")
                    .header("Authorization", "Bearer " + key)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(
                            Map.of(
                                    "model", model,
                                    "instructions", instructions,
                                    "input", input
                            )
                    )
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(Duration.ofSeconds(90));

            JsonNode root = mapper.readTree(raw);

            JsonNode output = root.path("output");

            StringBuilder text = new StringBuilder();

            if (output.isArray()) {

                for (JsonNode item : output) {

                    if ("message".equals(item.path("type").asText())) {

                        for (JsonNode content : item.path("content")) {

                            if ("output_text".equals(
                                    content.path("type").asText()
                            )) {
                                text.append(
                                        content.path("text").asText()
                                );
                            }
                        }
                    }
                }
            }

            if (!text.isEmpty()) {
                return text.toString();
            }

            if (root.has("output_text")) {
                return root.get("output_text").asText();
            }

            throw new IllegalStateException(
                    "OpenAI returned no text output"
            );

        } catch (WebClientResponseException e) {

            String body = e.getResponseBodyAsString();

            System.err.println(
                    "OpenAI HTTP " + e.getStatusCode().value()
                            + ": " + body
            );

            throw new IllegalStateException(
                    "OpenAI request failed with HTTP "
                            + e.getStatusCode().value()
                            + ": "
                            + body
            );

        } catch (Exception e) {

            e.printStackTrace();

            throw new IllegalStateException(
                    "LLM request failed: " + e.getMessage(),
                    e
            );
        }
    }
}