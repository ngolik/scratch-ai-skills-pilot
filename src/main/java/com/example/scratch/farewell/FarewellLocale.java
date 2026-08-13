package com.example.scratch.farewell;

public enum FarewellLocale {

    EN("en", "Goodbye, %s!"),
    ES("es", "¡Adiós, %s!"),
    DE("de", "Auf Wiedersehen, %s!");

    private static final FarewellLocale DEFAULT_LOCALE = EN;

    private final String code;
    private final String messageTemplate;

    FarewellLocale(String code, String messageTemplate) {
        this.code = code;
        this.messageTemplate = messageTemplate;
    }

    public String code() {
        return code;
    }

    public String formatMessage(String name) {
        return String.format(messageTemplate, name);
    }

    public static FarewellLocale resolve(String code) {
        if (code == null) {
            return DEFAULT_LOCALE;
        }
        for (FarewellLocale locale : values()) {
            if (locale.code.equals(code)) {
                return locale;
            }
        }
        throw new IllegalArgumentException("Unsupported locale: " + code);
    }
}
