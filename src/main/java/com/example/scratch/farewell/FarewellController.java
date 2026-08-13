package com.example.scratch.farewell;

import jakarta.validation.Valid;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Validation failures (@{@code Valid} / malformed JSON) on this controller are mapped to the
 * standard {@code 400} shape by {@code greeting.GreetingValidationExceptionHandler}, an
 * app-wide {@code @RestControllerAdvice} — no exception-handling code lives in this package.
 */
@RestController
public class FarewellController {

    static final String FAREWELLS_PATH = "/api/v1/farewells";

    private final FarewellService farewellService;

    public FarewellController(FarewellService farewellService) {
        this.farewellService = farewellService;
    }

    @PostMapping(path = FAREWELLS_PATH,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public FarewellResponse createFarewell(@Valid @RequestBody FarewellRequest request) {
        return farewellService.farewell(request);
    }
}
