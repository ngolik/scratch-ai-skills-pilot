package com.example.scratch.counter;

/**
 * Slug rules for a counter {@code name}: 1-40 lowercase letters/digits/hyphens, must not start
 * or end with a hyphen, must not contain {@code --}. Deliberately separate from
 * {@code com.example.scratch.validation.NameValidationConstants}, which governs person names for
 * greetings/farewells — a different rule set for a different feature.
 */
final class CounterNameConstants {

    static final String NAME_PATTERN = "^(?!.*--)[a-z0-9]([a-z0-9-]{0,38}[a-z0-9])?$";

    private CounterNameConstants() {
    }
}
