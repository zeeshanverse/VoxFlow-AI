package com.voxflow.interview;

import com.voxflow.assistant.security.RateLimitService;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RequestHeader;

import java.time.Duration;

@RestController
@RequestMapping("/api/interviews")
public class InterviewController {

    private final InterviewService service;
    private final RateLimitService rateLimitService;

    public InterviewController(
            InterviewService service,
            RateLimitService rateLimitService
    ) {
        this.service = service;
        this.rateLimitService = rateLimitService;
    }

    @PostMapping
    public InterviewService.Start start(
            @RequestParam(defaultValue = "Java Backend") String topic,
            @RequestHeader(value = "X-VoxFlow-Operation-Id", required = false) String operationId
    ) {
        return service.start(topic, operationId);
    }

    @PostMapping(
        value = "/{id}/answer",
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public InterviewService.AnswerResult answer(
            @PathVariable Long id,
            @RequestPart("audio") MultipartFile audio,
            Authentication authentication,
            @RequestHeader(value = "X-VoxFlow-Operation-Id", required = false) String operationId
    ) {
        boolean allowed = rateLimitService.isAllowed(
                "interview-answer:" + authentication.getName(),
                10,
                Duration.ofMinutes(1)
        );

        if (!allowed) {
            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Too many interview answer requests. Please try again later."
            );
        }

        return service.answer(id, audio, operationId);
    }

    @GetMapping("/{id}")
    public InterviewService.SessionResult get(@PathVariable Long id) {
        return service.get(id);
    }
}