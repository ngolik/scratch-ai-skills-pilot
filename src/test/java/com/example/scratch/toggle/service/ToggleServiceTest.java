package com.example.scratch.toggle.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.scratch.toggle.dto.ToggleResponse;
import com.example.scratch.toggle.repository.InMemoryToggleRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ToggleServiceTest {

    private ToggleService toggleService;

    @BeforeEach
    void setUp() {
        toggleService = new ToggleService(new InMemoryToggleRepository());
    }

    @Test
    void setToggle_WhenToggleIsNew_ReturnsEnabledValue() {
        ToggleResponse response = toggleService.setToggle("dark-mode", true);

        assertThat(response.name()).isEqualTo("dark-mode");
        assertThat(response.enabled()).isTrue();
        assertThat(response.updatedAt()).isNotNull();
    }

    @Test
    void getToggle_AfterSetToggle_ReturnsSameEnabledValue() {
        toggleService.setToggle("dark-mode", true);

        ToggleResponse response = toggleService.getToggle("dark-mode");

        assertThat(response.name()).isEqualTo("dark-mode");
        assertThat(response.enabled()).isTrue();
    }

    @Test
    void getToggle_AfterOverwriteFromTrueToFalse_ReflectsFalse() {
        toggleService.setToggle("dark-mode", true);
        toggleService.setToggle("dark-mode", false);

        ToggleResponse response = toggleService.getToggle("dark-mode");

        assertThat(response.enabled()).isFalse();
    }

    @Test
    void getToggle_WhenToggleUnknown_ThrowsToggleNotFoundException() {
        assertThatThrownBy(() -> toggleService.getToggle("missing"))
                .isInstanceOf(ToggleNotFoundException.class);
    }
}
