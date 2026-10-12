package com.voxflow.assistant.security;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OperationCancellationService {

    private static final long RETENTION_SECONDS = 600;

    private final Map<String, Instant> cancelled = new ConcurrentHashMap<>();

    public void cancel(String userKey, String operationId) {
        if (operationId == null || operationId.isBlank()) return;
        cleanup();
        cancelled.put(key(userKey, operationId), Instant.now());
    }

    public boolean isCancelled(String userKey, String operationId) {
        if (operationId == null || operationId.isBlank()) return false;
        cleanup();
        return cancelled.containsKey(key(userKey, operationId));
    }

    public void throwIfCancelled(String userKey, String operationId) {
        if (isCancelled(userKey, operationId)) {
            throw new OperationCancelledException();
        }
    }

    private String key(String userKey, String operationId) {
        return userKey + ":" + operationId;
    }

    private void cleanup() {
        Instant cutoff = Instant.now().minusSeconds(RETENTION_SECONDS);
        cancelled.entrySet().removeIf(entry -> entry.getValue().isBefore(cutoff));
    }
}
