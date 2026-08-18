package com.example.scratch.status.repository;

import java.util.Optional;

import com.example.scratch.status.entity.Status;

public interface StatusRepository {

    Status save(Status status);

    Optional<Status> findByName(String name);
}
