package com.example.scratch.status.repository;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.example.scratch.status.entity.Status;

public class InMemoryStatusRepository implements StatusRepository {

    private final ConcurrentMap<String, Status> statuses = new ConcurrentHashMap<>();

    @Override
    public Status save(Status status) {
        statuses.put(status.name(), status);
        return status;
    }

    @Override
    public Optional<Status> findByName(String name) {
        return Optional.ofNullable(statuses.get(name));
    }
}
