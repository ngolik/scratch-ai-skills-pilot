package com.example.scratch.farewell;

import java.time.Instant;

import org.springframework.stereotype.Service;

@Service
public class FarewellService {

    public FarewellResponse farewell(FarewellRequest request) {
        FarewellLocale locale = FarewellLocale.resolve(request.locale());
        String message = locale.formatMessage(request.name());
        return new FarewellResponse(message, request.name(), locale.code(), Instant.now());
    }
}
