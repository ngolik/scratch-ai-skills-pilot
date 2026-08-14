package com.example.scratch.notes.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.example.scratch.notes.application.NoteApplicationService;
import com.example.scratch.notes.infrastructure.InMemoryNoteRepository;
import com.jayway.jsonpath.JsonPath;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NoteController.class)
@Import({NoteApplicationService.class, InMemoryNoteRepository.class})
class NoteControllerTest {

    private static final String UUID_REGEX =
            "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createNote_WhenTextIsValid_ReturnsCreatedWithTrimmedTextAndUuidId() throws Exception {
        mockMvc.perform(post("/api/v1/notes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"text": "  Ship the plugin test  "}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.text").value("Ship the plugin test"))
                .andExpect(jsonPath("$.id").value(matchesPattern(UUID_REGEX)))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    void getNote_AfterCreate_ReturnsSameBody() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/api/v1/notes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"text": "Get after create"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        String createBody = createResult.getResponse().getContentAsString();
        String id = JsonPath.read(createBody, "$.id");

        mockMvc.perform(get("/api/v1/notes/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.text").value("Get after create"));
    }

    @Test
    void createNote_WhenTextIsBlank_ReturnsValidationFailed() throws Exception {
        mockMvc.perform(post("/api/v1/notes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"text": "   "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("validation_failed"))
                .andExpect(jsonPath("$.details[0].field").value("text"))
                .andExpect(jsonPath("$.details[0].message").value("must not be blank"));
    }

    @Test
    void createNote_WhenTextExceeds200Characters_ReturnsValidationFailed() throws Exception {
        String tooLongText = "a".repeat(201);

        mockMvc.perform(post("/api/v1/notes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\": \"" + tooLongText + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("validation_failed"))
                .andExpect(jsonPath("$.details[0].field").value("text"))
                .andExpect(jsonPath("$.details[0].message").value("must be at most 200 characters"));
    }

    @Test
    void getNote_WhenIdIsMalformed_ReturnsValidationFailed() throws Exception {
        mockMvc.perform(get("/api/v1/notes/{id}", "not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("validation_failed"))
                .andExpect(jsonPath("$.details[0].field").value("id"))
                .andExpect(jsonPath("$.details[0].message").value("must be a valid UUID"));
    }

    @Test
    void getNote_WhenIdIsWellFormedButUnknown_ReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/notes/{id}", "00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("not_found"))
                .andExpect(jsonPath("$.details[0].field").value("id"))
                .andExpect(jsonPath("$.details[0].message").value("note does not exist"));
    }
}
