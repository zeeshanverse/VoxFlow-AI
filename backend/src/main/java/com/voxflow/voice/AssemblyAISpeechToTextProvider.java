package com.voxflow.voice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

@Service
public class AssemblyAISpeechToTextProvider implements SpeechToTextProvider {

    private static final Duration UPLOAD_TIMEOUT = Duration.ofSeconds(20);
    private static final Duration TRANSCRIPT_CREATE_TIMEOUT = Duration.ofSeconds(20);
    private static final Duration POLL_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration TOTAL_POLL_TIMEOUT = Duration.ofSeconds(45);

    private final WebClient client;
    private final String key;
    private final ObjectMapper mapper = new ObjectMapper();

    public AssemblyAISpeechToTextProvider(
            WebClient client,
            @Value("${assemblyai.api-key:}") String key
    ) {
        this.client = client;
        this.key = key;
    }

    @Override
    public String transcribe(MultipartFile audio) {
        if (key == null || key.isBlank()) {
            throw new IllegalStateException("AssemblyAI is not configured: ASSEMBLYAI_API_KEY is missing");
        }
        if (audio == null || audio.isEmpty()) {
            throw new IllegalStateException("No audio was received");
        }

        try {
            long started = System.currentTimeMillis();

            String upload = client.post()
                    .uri("https://api.assemblyai.com/v2/upload")
                    .header("authorization", key)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .bodyValue(audio.getBytes())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(UPLOAD_TIMEOUT);

            String uploadUrl = mapper.readTree(upload).path("upload_url").asText();
            if (uploadUrl.isBlank()) {
                throw new IllegalStateException("AssemblyAI did not return an upload URL");
            }

            String job = client.post()
                    .uri("https://api.assemblyai.com/v2/transcript")
                    .header("authorization", key)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(Map.of("audio_url", uploadUrl))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(TRANSCRIPT_CREATE_TIMEOUT);

            String id = mapper.readTree(job).path("id").asText();
            if (id.isBlank()) {
                throw new IllegalStateException("AssemblyAI did not return a transcript ID");
            }

            Instant deadline = Instant.now().plus(TOTAL_POLL_TIMEOUT);

            while (Instant.now().isBefore(deadline)) {
                try {
                    String result = client.get()
                            .uri("https://api.assemblyai.com/v2/transcript/" + id)
                            .header("authorization", key)
                            .retrieve()
                            .bodyToMono(String.class)
                            .block(POLL_TIMEOUT);

                    JsonNode node = mapper.readTree(result);
                    String status = node.path("status").asText();

                    if ("completed".equals(status)) {
                        String text = node.path("text").asText("").trim();
                        System.out.println(
                                "AssemblyAI completed in "
                                        + (System.currentTimeMillis() - started)
                                        + " ms"
                        );
                        return text;
                    }

                    if ("error".equals(status)) {
                        throw new IllegalStateException(
                                "AssemblyAI transcription failed: "
                                        + node.path("error").asText("unknown error")
                        );
                    }
                } catch (WebClientResponseException e) {
                    int status = e.getStatusCode().value();
                    if (status == 401 || status == 403 || status == 400) {
                        throw new IllegalStateException(
                                "AssemblyAI request failed with HTTP " + status
                        );
                    }
                } catch (IllegalStateException e) {
                    throw e;
                } catch (RuntimeException ignored) {
                    // A transient polling timeout/connection reset is retried until the total deadline.
                }

                Thread.sleep(500);
            }

            throw new IllegalStateException("AssemblyAI transcription timed out after 45 seconds");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("AssemblyAI transcription interrupted");
        } catch (WebClientResponseException e) {
            throw new IllegalStateException(
                    "AssemblyAI request failed with HTTP " + e.getStatusCode().value()
            );
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            System.err.println("AssemblyAI error: " + e.getMessage());
            throw new IllegalStateException("AssemblyAI transcription failed");
        }
    }
}
