package com.example.scratch.label;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.stereotype.Service;

/**
 * Holds all label state in a single process-local map. {@link ConcurrentMap#compute} performs
 * the create-or-replace as one atomic per-key operation, so concurrent {@code PUT}s on the same
 * name never interleave-corrupt state (same pattern as {@code counter.CounterService}).
 */
@Service
public class LabelService {

    private static final int MAX_VALUE_LENGTH = 32;
    private static final String BLANK_VALUE = "must not be blank";
    private static final String TOO_LONG_VALUE = "must be at most 32 characters";

    private final ConcurrentMap<String, LabelResponse> labels = new ConcurrentHashMap<>();

    public LabelResponse setLabel(String name, String rawValue) {
        String value = validateValue(rawValue);
        return labels.compute(name, (key, existing) -> new LabelResponse(key, value, Instant.now()));
    }

    public LabelResponse getLabel(String name) {
        LabelResponse label = labels.get(name);
        if (label == null) {
            throw new LabelNotFoundException(name);
        }
        return label;
    }

    private static String validateValue(String rawValue) {
        String trimmed = rawValue.trim();
        if (trimmed.isEmpty()) {
            throw new LabelValueInvalidException(BLANK_VALUE);
        }
        if (trimmed.length() > MAX_VALUE_LENGTH) {
            throw new LabelValueInvalidException(TOO_LONG_VALUE);
        }
        return trimmed;
    }
}
