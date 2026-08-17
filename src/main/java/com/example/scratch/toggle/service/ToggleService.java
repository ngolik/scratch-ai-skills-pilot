package com.example.scratch.toggle.service;

import java.time.Instant;

import org.springframework.stereotype.Service;

import com.example.scratch.toggle.dto.ToggleResponse;
import com.example.scratch.toggle.entity.Toggle;
import com.example.scratch.toggle.repository.ToggleRepository;

@Service
public class ToggleService {

    private final ToggleRepository toggleRepository;

    public ToggleService(ToggleRepository toggleRepository) {
        this.toggleRepository = toggleRepository;
    }

    public ToggleResponse setToggle(String name, boolean enabled) {
        Toggle toggle = toggleRepository.save(new Toggle(name, enabled, Instant.now()));
        return toResponse(toggle);
    }

    public ToggleResponse getToggle(String name) {
        Toggle toggle = toggleRepository.findByName(name)
                .orElseThrow(() -> new ToggleNotFoundException(name));
        return toResponse(toggle);
    }

    private static ToggleResponse toResponse(Toggle toggle) {
        return new ToggleResponse(toggle.name(), toggle.enabled(), toggle.updatedAt());
    }
}
