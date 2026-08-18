package com.example.scratch.toggle.service;

public class ToggleNotFoundException extends RuntimeException {

    public ToggleNotFoundException(String name) {
        super("Toggle not found: " + name);
    }
}
