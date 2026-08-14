package com.example.scratch.counter;

import jakarta.validation.constraints.Pattern;

import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code name} constraint violations on this controller are mapped to the standard {@code 400}
 * shape by {@code greeting.GreetingValidationExceptionHandler}'s
 * {@code ConstraintViolationException} handler; "counter not found" is mapped to {@code 404} by
 * this package's own {@link CounterExceptionHandler}.
 */
@RestController
// Do not remove: routes @PathVariable constraint violations through the legacy AOP proxy path
// (ConstraintViolationException), which greeting.GreetingValidationExceptionHandler catches.
// Without it, Spring's native method-validation path raises HandlerMethodValidationException
// instead, nothing here catches that, and the 400 body silently changes from validation_failed
// to Spring's default ProblemDetail shape.
@Validated
public class CounterController {

    static final String COUNTER_PATH = "/api/v1/counters/{name}";
    static final String COUNTER_INCREMENTS_PATH = "/api/v1/counters/{name}/increments";

    private static final String NAME_PATTERN_MESSAGE = "must be 1-40 lowercase letters, digits, "
            + "or hyphens, and must not start or end with a hyphen or contain consecutive hyphens";

    private final CounterService counterService;

    public CounterController(CounterService counterService) {
        this.counterService = counterService;
    }

    @PostMapping(path = COUNTER_INCREMENTS_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
    public CounterResponse incrementCounter(
            @PathVariable
            @Pattern(regexp = CounterNameConstants.NAME_PATTERN, message = NAME_PATTERN_MESSAGE)
            String name) {
        return counterService.increment(name);
    }

    @GetMapping(path = COUNTER_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
    public CounterResponse getCounter(
            @PathVariable
            @Pattern(regexp = CounterNameConstants.NAME_PATTERN, message = NAME_PATTERN_MESSAGE)
            String name) {
        return counterService.get(name);
    }
}
