package com.voxflow.voice;

import com.voxflow.assistant.security.RateLimitService;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;

@RestController
@RequestMapping("/api/voice")
public class VoiceController {

    private final SpeechToTextProvider stt;
    private final TextToSpeechProvider tts;
    private final RateLimitService rateLimitService;

    public VoiceController(
            SpeechToTextProvider stt,
            TextToSpeechProvider tts,
            RateLimitService rateLimitService
    ) {
        this.stt = stt;
        this.tts = tts;
        this.rateLimitService = rateLimitService;
    }

    @PostMapping(
            value = "/transcribe",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public TranscriptResponse transcribe(
            @RequestPart("audio") MultipartFile audio,
            Authentication authentication
    ) {
        String key = "transcribe:" + authentication.getName();

        boolean allowed = rateLimitService.isAllowed(
                key,
                10,
                Duration.ofMinutes(1)
        );

        if (!allowed) {
            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Too many transcription requests. Please try again later."
            );
        }

        return new TranscriptResponse(stt.transcribe(audio));
    }

    @PostMapping(
        value = "/speak",
        consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<byte[]> speak(
            @RequestBody SpeakRequest request,
            Authentication authentication
    ) {
        String key = "speak:" + authentication.getName();

        boolean allowed = rateLimitService.isAllowed(
                key,
                10,
                Duration.ofMinutes(1)
        );

        if (!allowed) {
            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Too many speech requests. Please try again later."
            );
        }

        if (request.text() == null || request.text().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Speech text cannot be empty."
            );
        }

        if (request.text().length() > 5000) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Speech text is too long. Maximum length is 5000 characters."
            );
        }

        return ResponseEntity.ok()
                .contentType(MediaType.valueOf("audio/mpeg"))
                .body(tts.synthesize(request.text()));
    }

    public record TranscriptResponse(String text) {}
    public record SpeakRequest(String text) {}
}