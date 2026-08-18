package com.example.scratch.label;

/**
 * Slug rules for a label {@code name}: 1-40 lowercase letters/digits/hyphens, must not start
 * or end with a hyphen, must not contain {@code --}. Deliberately separate from
 * {@code com.example.scratch.validation.NameValidationConstants} and every other feature's
 * name-pattern constant (one per feature is the established convention in this codebase).
 */
final class LabelNameConstants {

    static final String NAME_PATTERN = "^(?!.*--)[a-z0-9]([a-z0-9-]{0,38}[a-z0-9])?$";

    private LabelNameConstants() {
    }
}
