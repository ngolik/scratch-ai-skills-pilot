package com.example.scratch.status.service;

public class StatusMessageInvalidException extends RuntimeException {

    public StatusMessageInvalidException(String reason) {
        super(reason);
    }
}
