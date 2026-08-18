package com.example.scratch.label;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;

import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code name} constraint violations and missing {@code value} on this controller are mapped to
 * the standard {@code 400} shape by {@code greeting.GreetingValidationExceptionHandler};
 * blank/too-long-after-trim {@code value} ({@code 400}) and "label not found" ({@code 404}) are
 * mapped by this package's own {@link LabelExceptionHandler}.
 */
@RestController
// Do not remove: routes @PathVariable constraint violations through the legacy AOP proxy path
// (ConstraintViolationException), which greeting.GreetingValidationExceptionHandler catches.
// Without it, Spring's native method-validation path raises HandlerMethodValidationException
// instead, nothing here catches that, and the 400 body silently changes from validation_failed
// to Spring's default ProblemDetail shape.
@Validated
public class LabelController {

    static final String LABEL_PATH = "/api/v1/labels/{name}";

    private static final String NAME_PATTERN_MESSAGE = "must be 1-40 lowercase letters, digits, "
            + "or hyphens, and must not start or end with a hyphen or contain consecutive hyphens";

    private final LabelService labelService;

    public LabelController(LabelService labelService) {
        this.labelService = labelService;
    }

    @PutMapping(path = LABEL_PATH,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public LabelResponse setLabel(
            @PathVariable
            @Pattern(regexp = LabelNameConstants.NAME_PATTERN, message = NAME_PATTERN_MESSAGE)
            String name,
            @Valid @RequestBody LabelRequest request) {
        return labelService.setLabel(name, request.value());
    }

    @GetMapping(path = LABEL_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
    public LabelResponse getLabel(
            @PathVariable
            @Pattern(regexp = LabelNameConstants.NAME_PATTERN, message = NAME_PATTERN_MESSAGE)
            String name) {
        return labelService.getLabel(name);
    }
}
