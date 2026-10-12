package com.voxflow.voice;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.Map;

@Service
public class ElevenLabsTextToSpeechProvider implements TextToSpeechProvider {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

    private final WebClient client;
    private final String key;
    private final String voice;
    private final String model;

    public ElevenLabsTextToSpeechProvider(
            WebClient client,
            @Value("${elevenlabs.api-key:}") String key,
            @Value("${elevenlabs.voice-id:}") String voice,
            @Value("${elevenlabs.model-id:eleven_multilingual_v2}") String model
    ) {
        this.client = client;
        this.key = key;
        this.voice = voice;
        this.model = model;
    }

    @Override
    public byte[] synthesize(String text) {
        if (key == null || key.isBlank()) {
            throw new IllegalStateException("ElevenLabs is not configured: ELEVENLABS_API_KEY is missing");
        }
        if (voice == null || voice.isBlank()) {
            throw new IllegalStateException("ElevenLabs is not configured: ELEVENLABS_VOICE_ID is missing");
        }

        try {
            long started = System.currentTimeMillis();

            byte[] audio = client.post()
                    .uri(
                            "https://api.elevenlabs.io/v1/text-to-speech/{voice}",
                            voice
                    )
                    .header("xi-api-key", key)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.valueOf("audio/mpeg"))
                    .bodyValue(Map.of(
                            "text", text,
                            "model_id", model
                    ))
                    .retrieve()
                    .bodyToMono(byte[].class)
                    .block(REQUEST_TIMEOUT);

            System.out.println(
                    "ElevenLabs completed in "
                            + (System.currentTimeMillis() - started)
                            + " ms"
            );

            return audio;
        } catch (WebClientResponseException e) {
            throw new IllegalStateException(
                    "ElevenLabs request failed with HTTP " + e.getStatusCode().value()
            );
        } catch (Exception e) {
            System.err.println("ElevenLabs error: " + e.getMessage());
            throw new IllegalStateException("ElevenLabs speech generation failed");
        }
    }
}
