package com.example.scratch.status.service;

import java.time.Instant;

import org.springframework.stereotype.Service;

import com.example.scratch.status.dto.StatusResponse;
import com.example.scratch.status.entity.Status;
import com.example.scratch.status.repository.StatusRepository;

@Service
public class StatusService {

    private static final int MAX_MESSAGE_LENGTH = 80;
    private static final String BLANK_MESSAGE = "must not be blank";
    private static final String TOO_LONG_MESSAGE = "must be at most 80 characters";

    private final StatusRepository statusRepository;

    public StatusService(StatusRepository statusRepository) {
        this.statusRepository = statusRepository;
    }

    public StatusResponse setStatus(String name, String rawMessage) {
        String message = validateMessage(rawMessage);
        Status status = statusRepository.save(new Status(name, message, Instant.now()));
        return toResponse(status);
    }

    public StatusResponse getStatus(String name) {
        Status status = statusRepository.findByName(name)
                .orElseThrow(() -> new StatusNotFoundException(name));
        return toResponse(status);
    }

    private static String validateMessage(String rawMessage) {
        String trimmed = rawMessage.trim();
        if (trimmed.isEmpty()) {
            throw new StatusMessageInvalidException(BLANK_MESSAGE);
        }
        if (trimmed.length() > MAX_MESSAGE_LENGTH) {
            throw new StatusMessageInvalidException(TOO_LONG_MESSAGE);
        }
        return trimmed;
    }

    private static StatusResponse toResponse(Status status) {
        return new StatusResponse(status.name(), status.message(), status.updatedAt());
    }
}
