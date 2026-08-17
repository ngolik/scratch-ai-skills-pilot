package com.example.scratch.toggle.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;

import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.scratch.toggle.dto.SetToggleRequest;
import com.example.scratch.toggle.dto.ToggleResponse;
import com.example.scratch.toggle.service.ToggleService;

/**
 * {@code name} constraint violations on this controller are mapped to the standard {@code 400}
 * shape by {@code greeting.GreetingValidationExceptionHandler}'s
 * {@code ConstraintViolationException} handler; "toggle not found" is mapped to {@code 404} by
 * this package's own {@link ToggleExceptionHandler}.
 */
@RestController
// Do not remove: routes @PathVariable constraint violations through the legacy AOP proxy path
// (ConstraintViolationException), which greeting.GreetingValidationExceptionHandler catches.
// Without it, Spring's native method-validation path raises HandlerMethodValidationException
// instead, nothing here catches that, and the 400 body silently changes from validation_failed
// to Spring's default ProblemDetail shape.
@Validated
public class ToggleController {

    static final String TOGGLE_PATH = "/api/v1/toggles/{name}";

    private static final String NAME_PATTERN_MESSAGE = "must be 1-40 lowercase letters, digits, "
            + "or hyphens, and must not start or end with a hyphen or contain consecutive hyphens";

    private final ToggleService toggleService;

    public ToggleController(ToggleService toggleService) {
        this.toggleService = toggleService;
    }

    @PutMapping(path = TOGGLE_PATH,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ToggleResponse setToggle(
            @PathVariable
            @Pattern(regexp = ToggleNameConstants.NAME_PATTERN, message = NAME_PATTERN_MESSAGE)
            String name,
            @Valid @RequestBody SetToggleRequest request) {
        return toggleService.setToggle(name, request.enabled());
    }

    @GetMapping(path = TOGGLE_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
    public ToggleResponse getToggle(
            @PathVariable
            @Pattern(regexp = ToggleNameConstants.NAME_PATTERN, message = NAME_PATTERN_MESSAGE)
            String name) {
        return toggleService.getToggle(name);
    }
}
