package com.example.scratch.status.service;

public class StatusNotFoundException extends RuntimeException {

    public StatusNotFoundException(String name) {
        super("Status not found: " + name);
    }
}
