package com.voxflow.voice;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController @RequestMapping("/api/voice")
public class VoiceController {
    private final SpeechToTextProvider stt; private final TextToSpeechProvider tts;
    public VoiceController(SpeechToTextProvider stt,TextToSpeechProvider tts){this.stt=stt;this.tts=tts;}

    @PostMapping(value="/transcribe",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public TranscriptResponse transcribe(@RequestPart("audio") MultipartFile audio){return new TranscriptResponse(stt.transcribe(audio));}

    @PostMapping(value="/speak",consumes=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<byte[]> speak(@RequestBody SpeakRequest request){
        return ResponseEntity.ok().contentType(MediaType.valueOf("audio/mpeg")).body(tts.synthesize(request.text()));
    }
    public record TranscriptResponse(String text){}
    public record SpeakRequest(String text){}
}
