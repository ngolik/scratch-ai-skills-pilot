package com.example.scratch.toggle.repository;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.example.scratch.toggle.entity.Toggle;

public class InMemoryToggleRepository implements ToggleRepository {

    private final ConcurrentMap<String, Toggle> toggles = new ConcurrentHashMap<>();

    @Override
    public Toggle save(Toggle toggle) {
        toggles.put(toggle.name(), toggle);
        return toggle;
    }

    @Override
    public Optional<Toggle> findByName(String name) {
        return Optional.ofNullable(toggles.get(name));
    }
}
