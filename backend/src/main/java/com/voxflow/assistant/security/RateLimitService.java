package com.voxflow.assistant.security;

import io.github.bucket4j.Bucket;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitService {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public boolean isAllowed(String key, int capacity, Duration refillPeriod) {
        Bucket bucket = buckets.computeIfAbsent(
                key,
                ignored -> createBucket(capacity, refillPeriod)
        );

        return bucket.tryConsume(1);
    }

    private Bucket createBucket(int capacity, Duration refillPeriod) {
        return Bucket.builder()
                .addLimit(limit -> limit
                        .capacity(capacity)
                        .refillIntervally(capacity, refillPeriod))
                .build();
    }
}