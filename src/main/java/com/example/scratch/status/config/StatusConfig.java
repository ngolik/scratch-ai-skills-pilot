package com.example.scratch.status.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.example.scratch.status.repository.InMemoryStatusRepository;
import com.example.scratch.status.repository.StatusRepository;

@Configuration
public class StatusConfig {

    @Bean
    public StatusRepository statusRepository() {
        return new InMemoryStatusRepository();
    }
}
