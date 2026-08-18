package com.example.scratch.status.controller;

/**
 * Slug rules for a status {@code name}: 1-40 lowercase letters/digits/hyphens, must not start
 * or end with a hyphen, must not contain {@code --}. Deliberately separate from
 * {@code com.example.scratch.validation.NameValidationConstants} (person names) and the other
 * features' name-pattern constants (each feature owns its own copy).
 */
final class StatusNameConstants {

    static final String NAME_PATTERN = "^(?!.*--)[a-z0-9]([a-z0-9-]{0,38}[a-z0-9])?$";

    private StatusNameConstants() {
    }
}
