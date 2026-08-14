package com.example.scratch.counter;

public class CounterNotFoundException extends RuntimeException {

    public CounterNotFoundException(String name) {
        super("Counter not found: " + name);
    }
}
