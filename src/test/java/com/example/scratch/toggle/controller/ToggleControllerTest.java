package com.example.scratch.toggle.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import com.example.scratch.toggle.repository.InMemoryToggleRepository;
import com.example.scratch.toggle.service.ToggleService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ToggleController.class)
@Import({ToggleService.class, InMemoryToggleRepository.class})
class ToggleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void setToggle_WhenNameIsNew_CreatesAndReturnsEnabledTrue() throws Exception {
        mockMvc.perform(put("/api/v1/toggles/{name}", "dark-mode")
                        .contentType("application/json")
                        .content("{\"enabled\": true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("dark-mode"))
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.updatedAt").exists());
    }

    @Test
    void getToggle_AfterSet_ReturnsSameEnabledValue() throws Exception {
        String name = "get-after-set";
        mockMvc.perform(put("/api/v1/toggles/{name}", name)
                .contentType("application/json")
                .content("{\"enabled\": true}"));

        mockMvc.perform(get("/api/v1/toggles/{name}", name))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(name))
                .andExpect(jsonPath("$.enabled").value(true));
    }

    @Test
    void setToggle_WhenOverwritingTrueToFalse_ReturnsFalseOnSubsequentGet() throws Exception {
        String name = "overwrite-toggle";
        mockMvc.perform(put("/api/v1/toggles/{name}", name)
                .contentType("application/json")
                .content("{\"enabled\": true}"));

        mockMvc.perform(put("/api/v1/toggles/{name}", name)
                        .contentType("application/json")
                        .content("{\"enabled\": false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));

        mockMvc.perform(get("/api/v1/toggles/{name}", name))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));
    }

    @Test
    void getToggle_WhenNameUnknown_ReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/toggles/{name}", "never-set"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("not_found"))
                .andExpect(jsonPath("$.details[0].field").value("name"))
                .andExpect(jsonPath("$.details[0].message").value("toggle does not exist"));
    }

    @Test
    void setToggle_WhenNameIsUppercase_ReturnsValidationFailed() throws Exception {
        mockMvc.perform(put("/api/v1/toggles/{name}", "Dark-Mode")
                        .contentType("application/json")
                        .content("{\"enabled\": true}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("validation_failed"))
                .andExpect(jsonPath("$.details[0].field").value("name"));
    }

    @Test
    void setToggle_WhenNameStartsWithHyphen_ReturnsValidationFailed() throws Exception {
        mockMvc.perform(put("/api/v1/toggles/{name}", "-dark-mode")
                        .contentType("application/json")
                        .content("{\"enabled\": true}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("validation_failed"))
                .andExpect(jsonPath("$.details[0].field").value("name"));
    }

    @Test
    void setToggle_WhenNameHasConsecutiveHyphens_ReturnsValidationFailed() throws Exception {
        mockMvc.perform(put("/api/v1/toggles/{name}", "dark--mode")
                        .contentType("application/json")
                        .content("{\"enabled\": true}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("validation_failed"))
                .andExpect(jsonPath("$.details[0].field").value("name"));
    }

    @Test
    void setToggle_WhenNameExceeds40Characters_ReturnsValidationFailed() throws Exception {
        String tooLongName = "a".repeat(41);

        mockMvc.perform(put("/api/v1/toggles/{name}", tooLongName)
                        .contentType("application/json")
                        .content("{\"enabled\": true}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("validation_failed"))
                .andExpect(jsonPath("$.details[0].field").value("name"));
    }

    @Test
    void setToggle_WhenEnabledIsMissing_ReturnsValidationFailed() throws Exception {
        mockMvc.perform(put("/api/v1/toggles/{name}", "dark-mode")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("validation_failed"))
                .andExpect(jsonPath("$.details[0].field").value("enabled"));
    }

    @Test
    void setToggle_WhenEnabledIsNotBoolean_ReturnsValidationFailed() throws Exception {
        mockMvc.perform(put("/api/v1/toggles/{name}", "dark-mode")
                        .contentType("application/json")
                        .content("{\"enabled\": \"yes\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("validation_failed"));
    }

    @Test
    void getToggle_WhenNameInvalid_ReturnsValidationFailed() throws Exception {
        mockMvc.perform(get("/api/v1/toggles/{name}", "Invalid_Name"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("validation_failed"))
                .andExpect(jsonPath("$.details[0].field").value("name"));
    }
}
