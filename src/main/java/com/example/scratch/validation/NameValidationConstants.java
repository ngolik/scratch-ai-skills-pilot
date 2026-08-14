package com.example.scratch.validation;

/**
 * Shared name/locale validation rules for both {@code greeting.GreetingRequest} and
 * {@code farewell.FarewellRequest}, so the two endpoints' input contracts cannot drift apart
 * silently — a change here is a change for both.
 */
public final class NameValidationConstants {

    public static final int MAX_NAME_LENGTH = 40;
    public static final String NAME_PATTERN = "^[\\p{L}\\s'-]*$";
    public static final String LOCALE_PATTERN = "^(en|es|de)$";

    private NameValidationConstants() {
    }
}
