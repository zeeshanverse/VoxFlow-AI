package com.voxflow.voice;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import java.time.Duration;
import java.util.Map;

@Service
public class ElevenLabsTextToSpeechProvider implements TextToSpeechProvider {
    private final WebClient client; private final String key,voice,model;
    public ElevenLabsTextToSpeechProvider(WebClient client,@Value("${elevenlabs.api-key:}") String key,
        @Value("${elevenlabs.voice-id:}") String voice,@Value("${elevenlabs.model-id:eleven_multilingual_v2}") String model){
        this.client=client;this.key=key;this.voice=voice;this.model=model;
    }
    public byte[] synthesize(String text){
        if(key==null||key.isBlank()) throw new IllegalStateException("ELEVENLABS_API_KEY is not configured");
        if(voice==null||voice.isBlank()) throw new IllegalStateException("ELEVENLABS_VOICE_ID is not configured");
        return client.post().uri("https://api.elevenlabs.io/v1/text-to-speech/{voice}",voice)
                .header("xi-api-key",key).contentType(MediaType.APPLICATION_JSON).accept(MediaType.valueOf("audio/mpeg"))
                .bodyValue(Map.of("text",text,"model_id",model))
                .retrieve().bodyToMono(byte[].class).block(Duration.ofSeconds(60));
    }
}
