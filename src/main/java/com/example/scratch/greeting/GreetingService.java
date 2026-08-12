package com.example.scratch.greeting;

import java.time.Instant;

import org.springframework.stereotype.Service;

@Service
public class GreetingService {

    public GreetingResponse greet(GreetingRequest request) {
        GreetingLocale locale = GreetingLocale.resolve(request.locale());
        String message = locale.formatMessage(request.name());
        return new GreetingResponse(message, request.name(), locale.code(), Instant.now());
    }
}
