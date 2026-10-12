package com.voxflow.assistant.security;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/operations")
public class OperationController {

    private final OperationCancellationService cancellations;

    public OperationController(OperationCancellationService cancellations) {
        this.cancellations = cancellations;
    }

    @PostMapping("/cancel")
    public void cancel(
            @RequestHeader("X-VoxFlow-Operation-Id") String operationId,
            Authentication authentication
    ) {
        cancellations.cancel(authentication.getName(), operationId);
    }
}
