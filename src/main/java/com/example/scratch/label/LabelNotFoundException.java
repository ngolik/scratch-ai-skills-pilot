package com.example.scratch.label;

public class LabelNotFoundException extends RuntimeException {

    public LabelNotFoundException(String name) {
        super("Label not found: " + name);
    }
}
