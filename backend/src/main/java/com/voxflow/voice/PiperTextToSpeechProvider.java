package com.voxflow.voice;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.Map;

@Service
public class PiperTextToSpeechProvider implements TextToSpeechProvider {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(60);

    private final WebClient client;
    private final String piperUrl;
    private final String piperToken;

    public PiperTextToSpeechProvider(
            WebClient client,
            @Value("${piper.url:http://127.0.0.1:5000}") String piperUrl,
            @Value("${piper.token:}") String piperToken
    ) {
        this.client = client;
        this.piperUrl = piperUrl;
        this.piperToken = piperToken;
    }

    @Override
    public byte[] synthesize(String text) {

        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Speech text cannot be empty");
        }

        try {
            long started = System.currentTimeMillis();

            var requestBuilder = client.post()
                    .uri(piperUrl + "/synthesize")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.valueOf("audio/wav"));

            if (piperToken != null && !piperToken.isBlank()) {
                requestBuilder = requestBuilder.header("X-Piper-Token", piperToken);
            }

            byte[] audio = requestBuilder
                    .bodyValue(Map.of(
                            "text", text
                    ))
                    .retrieve()
                    .bodyToMono(byte[].class)
                    .block(REQUEST_TIMEOUT);

            System.out.println(
                    "Piper TTS completed in "
                            + (System.currentTimeMillis() - started)
                            + " ms"
            );

            if (audio == null || audio.length == 0) {
                throw new IllegalStateException(
                        "Piper returned empty audio"
                );
            }

            return audio;

        } catch (WebClientResponseException e) {

            System.err.println(
                    "Piper request failed: HTTP "
                            + e.getStatusCode().value()
                            + " - "
                            + e.getResponseBodyAsString()
            );

            throw new IllegalStateException(
                    "Piper TTS request failed with HTTP "
                            + e.getStatusCode().value()
            );

        } catch (Exception e) {

            System.err.println(
                    "Piper TTS error: " + e.getMessage()
            );

            throw new IllegalStateException(
                    "Piper speech generation failed"
            );
        }
    }
}