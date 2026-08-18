package com.example.scratch.status.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;

import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.scratch.status.dto.SetStatusRequest;
import com.example.scratch.status.dto.StatusResponse;
import com.example.scratch.status.service.StatusService;

/**
 * {@code name} constraint violations and missing {@code message} on this controller are mapped
 * to the standard {@code 400} shape by {@code greeting.GreetingValidationExceptionHandler};
 * blank/too-long-after-trim {@code message} ({@code 400}) and "status not found" ({@code 404})
 * are mapped by this package's own {@link StatusExceptionHandler}.
 */
@RestController
// Do not remove: routes @PathVariable constraint violations through the legacy AOP proxy path
// (ConstraintViolationException), which greeting.GreetingValidationExceptionHandler catches.
// Without it, Spring's native method-validation path raises HandlerMethodValidationException
// instead, nothing here catches that, and the 400 body silently changes from validation_failed
// to Spring's default ProblemDetail shape.
@Validated
public class StatusController {

    static final String STATUS_PATH = "/api/v1/statuses/{name}";

    private static final String NAME_PATTERN_MESSAGE = "must be 1-40 lowercase letters, digits, "
            + "or hyphens, and must not start or end with a hyphen or contain consecutive hyphens";

    private final StatusService statusService;

    public StatusController(StatusService statusService) {
        this.statusService = statusService;
    }

    @PutMapping(path = STATUS_PATH,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public StatusResponse setStatus(
            @PathVariable
            @Pattern(regexp = StatusNameConstants.NAME_PATTERN, message = NAME_PATTERN_MESSAGE)
            String name,
            @Valid @RequestBody SetStatusRequest request) {
        return statusService.setStatus(name, request.message());
    }

    @GetMapping(path = STATUS_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
    public StatusResponse getStatus(
            @PathVariable
            @Pattern(regexp = StatusNameConstants.NAME_PATTERN, message = NAME_PATTERN_MESSAGE)
            String name) {
        return statusService.getStatus(name);
    }
}
