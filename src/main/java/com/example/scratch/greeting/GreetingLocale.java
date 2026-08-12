package com.example.scratch.greeting;

public enum GreetingLocale {

    EN("en", "Hello, %s!"),
    ES("es", "¡Hola, %s!"),
    DE("de", "Hallo, %s!");

    private static final GreetingLocale DEFAULT_LOCALE = EN;

    private final String code;
    private final String messageTemplate;

    GreetingLocale(String code, String messageTemplate) {
        this.code = code;
        this.messageTemplate = messageTemplate;
    }

    public String code() {
        return code;
    }

    public String formatMessage(String name) {
        return String.format(messageTemplate, name);
    }

    public static GreetingLocale resolve(String code) {
        if (code == null) {
            return DEFAULT_LOCALE;
        }
        for (GreetingLocale locale : values()) {
            if (locale.code.equals(code)) {
                return locale;
            }
        }
        throw new IllegalArgumentException("Unsupported locale: " + code);
    }
}
