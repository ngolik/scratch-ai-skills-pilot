package com.example.scratch.counter;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.stereotype.Service;

/**
 * Holds all counter state in a single process-local map. {@link ConcurrentMap#compute} performs
 * the create-or-increment as one atomic per-key operation, so concurrent increments on the same
 * name never lose an update — no explicit locking needed.
 */
@Service
public class CounterService {

    private final ConcurrentMap<String, CounterResponse> counters = new ConcurrentHashMap<>();

    public CounterResponse increment(String name) {
        return counters.compute(name, (key, existing) -> {
            long newValue = existing == null ? 1L : existing.value() + 1L;
            return new CounterResponse(key, newValue, Instant.now());
        });
    }

    public CounterResponse get(String name) {
        CounterResponse counter = counters.get(name);
        if (counter == null) {
            throw new CounterNotFoundException(name);
        }
        return counter;
    }
}
