package com.example.scratch.greeting;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GreetingController.class)
@Import(GreetingService.class)
class GreetingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createGreeting_WhenLocaleIsEn_ReturnsEnglishGreeting() throws Exception {
        mockMvc.perform(post(GreetingController.GREETINGS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Ada", "locale": "en"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Hello, Ada!"))
                .andExpect(jsonPath("$.name").value("Ada"))
                .andExpect(jsonPath("$.locale").value("en"))
                .andExpect(jsonPath("$.generatedAt").exists());
    }

    @Test
    void createGreeting_WhenLocaleIsEs_ReturnsSpanishGreeting() throws Exception {
        mockMvc.perform(post(GreetingController.GREETINGS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Ada", "locale": "es"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("¡Hola, Ada!"))
                .andExpect(jsonPath("$.locale").value("es"));
    }

    @Test
    void createGreeting_WhenLocaleIsDe_ReturnsGermanGreeting() throws Exception {
        mockMvc.perform(post(GreetingController.GREETINGS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Ada", "locale": "de"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Hallo, Ada!"))
                .andExpect(jsonPath("$.locale").value("de"));
    }

    @Test
    void createGreeting_WhenLocaleOmitted_DefaultsToEnglishGreeting() throws Exception {
        mockMvc.perform(post(GreetingController.GREETINGS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Ada"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Hello, Ada!"))
                .andExpect(jsonPath("$.locale").value("en"));
    }

    @Test
    void createGreeting_WhenNameIsBlank_ReturnsValidationFailed() throws Exception {
        mockMvc.perform(post(GreetingController.GREETINGS_PATH)
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
    void createGreeting_WhenNameTooLong_ReturnsValidationFailed() throws Exception {
        String tooLongName = "A".repeat(41);
        mockMvc.perform(post(GreetingController.GREETINGS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"" + tooLongName + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("validation_failed"))
                .andExpect(jsonPath("$.details[0].field").value("name"))
                .andExpect(jsonPath("$.details[0].message").value("must be at most 40 characters"));
    }

    @Test
    void createGreeting_WhenNameHasInvalidCharacters_ReturnsValidationFailed() throws Exception {
        mockMvc.perform(post(GreetingController.GREETINGS_PATH)
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
    void createGreeting_WhenNameHasAccentedCharacters_ReturnsGreeting() throws Exception {
        mockMvc.perform(post(GreetingController.GREETINGS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "José", "locale": "es"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("¡Hola, José!"))
                .andExpect(jsonPath("$.name").value("José"));
    }

    @Test
    void createGreeting_WhenBodyIsMalformed_ReturnsValidationFailed() throws Exception {
        mockMvc.perform(post(GreetingController.GREETINGS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ not valid json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("validation_failed"))
                .andExpect(jsonPath("$.details[0].field").value("body"));
    }

    @Test
    void createGreeting_WhenLocaleUnsupported_ReturnsValidationFailed() throws Exception {
        mockMvc.perform(post(GreetingController.GREETINGS_PATH)
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
