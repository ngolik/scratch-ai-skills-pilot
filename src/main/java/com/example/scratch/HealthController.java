package com.example.scratch;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    private static final String HEALTH_PATH = "/health";
    private static final String OK_RESPONSE_BODY = "ok";

    @GetMapping(path = HEALTH_PATH, produces = MediaType.TEXT_PLAIN_VALUE)
    public String health() {
        return OK_RESPONSE_BODY;
    }
}
