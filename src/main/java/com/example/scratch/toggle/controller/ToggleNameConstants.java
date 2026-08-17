package com.example.scratch.toggle.controller;

/**
 * Slug rules for a toggle {@code name}: 1-40 lowercase letters/digits/hyphens, must not start
 * or end with a hyphen, must not contain {@code --}. Deliberately separate from
 * {@code com.example.scratch.validation.NameValidationConstants} (person names) and
 * {@code com.example.scratch.counter.CounterNameConstants} (a different feature's slug rules).
 */
final class ToggleNameConstants {

    static final String NAME_PATTERN = "^(?!.*--)[a-z0-9]([a-z0-9-]{0,38}[a-z0-9])?$";

    private ToggleNameConstants() {
    }
}
