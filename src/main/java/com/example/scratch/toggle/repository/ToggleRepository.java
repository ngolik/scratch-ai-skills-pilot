package com.example.scratch.toggle.repository;

import java.util.Optional;

import com.example.scratch.toggle.entity.Toggle;

public interface ToggleRepository {

    Toggle save(Toggle toggle);

    Optional<Toggle> findByName(String name);
}
