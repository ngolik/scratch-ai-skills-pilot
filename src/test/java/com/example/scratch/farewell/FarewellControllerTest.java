package com.example.scratch.farewell;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FarewellController.class)
@Import(FarewellService.class)
class FarewellControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createFarewell_WhenLocaleIsEn_ReturnsEnglishFarewell() throws Exception {
        mockMvc.perform(post(FarewellController.FAREWELLS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Ada", "locale": "en"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Goodbye, Ada!"))
                .andExpect(jsonPath("$.name").value("Ada"))
                .andExpect(jsonPath("$.locale").value("en"))
                .andExpect(jsonPath("$.generatedAt").exists());
    }

    @Test
    void createFarewell_WhenLocaleIsEs_ReturnsSpanishFarewell() throws Exception {
        mockMvc.perform(post(FarewellController.FAREWELLS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Ada", "locale": "es"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("¡Adiós, Ada!"))
                .andExpect(jsonPath("$.locale").value("es"));
    }

    @Test
    void createFarewell_WhenLocaleIsDe_ReturnsGermanFarewell() throws Exception {
        mockMvc.perform(post(FarewellController.FAREWELLS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Ada", "locale": "de"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Auf Wiedersehen, Ada!"))
                .andExpect(jsonPath("$.locale").value("de"));
    }

    @Test
    void createFarewell_WhenLocaleOmitted_DefaultsToEnglishFarewell() throws Exception {
        mockMvc.perform(post(FarewellController.FAREWELLS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Ada"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Goodbye, Ada!"))
                .andExpect(jsonPath("$.locale").value("en"));
    }

    @Test
    void createFarewell_WhenNameIsBlank_ReturnsValidationFailed() throws Exception {
        mockMvc.perform(post(FarewellController.FAREWELLS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": ""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("validation_failed"))
                .andExpect(jsonPath("$.details[0].field").value("name"))
                .andExpect(jsonPath("$.details[0].message").value("must not be blank"));
    }

    @Test
    void createFarewell_WhenNameTooLong_ReturnsValidationFailed() throws Exception {
        String tooLongName = "A".repeat(41);
        mockMvc.perform(post(FarewellController.FAREWELLS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"" + tooLongName + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("validation_failed"))
                .andExpect(jsonPath("$.details[0].field").value("name"))
                .andExpect(jsonPath("$.details[0].message").value("must be at most 40 characters"));
    }

    @Test
    void createFarewell_WhenNameHasInvalidCharacters_ReturnsValidationFailed() throws Exception {
        mockMvc.perform(post(FarewellController.FAREWELLS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Ada123"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("validation_failed"))
                .andExpect(jsonPath("$.details[0].field").value("name"))
                .andExpect(jsonPath("$.details[0].message")
                        .value("must contain only letters, spaces, hyphens, and apostrophes"));
    }

    @Test
    void createFarewell_WhenNameHasAccentedCharacters_ReturnsFarewell() throws Exception {
        mockMvc.perform(post(FarewellController.FAREWELLS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "José", "locale": "es"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("¡Adiós, José!"))
                .andExpect(jsonPath("$.name").value("José"));
    }

    @Test
    void createFarewell_WhenBodyIsMalformed_ReturnsValidationFailed() throws Exception {
        mockMvc.perform(post(FarewellController.FAREWELLS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ not valid json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("validation_failed"))
                .andExpect(jsonPath("$.details[0].field").value("body"));
    }

    @Test
    void createFarewell_WhenLocaleUnsupported_ReturnsValidationFailed() throws Exception {
        mockMvc.perform(post(FarewellController.FAREWELLS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Ada", "locale": "fr"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("validation_failed"))
                .andExpect(jsonPath("$.details[0].field").value("locale"))
                .andExpect(jsonPath("$.details[0].message").value("must be one of: en, es, de"));
    }
}
