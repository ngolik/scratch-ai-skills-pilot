package com.example.scratch.toggle.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.example.scratch.toggle.repository.InMemoryToggleRepository;
import com.example.scratch.toggle.repository.ToggleRepository;

@Configuration
public class ToggleConfig {

    @Bean
    public ToggleRepository toggleRepository() {
        return new InMemoryToggleRepository();
    }
}
