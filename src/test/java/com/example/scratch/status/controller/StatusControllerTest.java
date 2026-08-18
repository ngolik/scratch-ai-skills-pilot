package com.example.scratch.status.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import com.example.scratch.status.repository.InMemoryStatusRepository;
import com.example.scratch.status.service.StatusService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StatusController.class)
@Import({StatusService.class, InMemoryStatusRepository.class})
class StatusControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void setStatus_WhenNameIsNew_CreatesAndReturnsMessage() throws Exception {
        mockMvc.perform(put("/api/v1/statuses/{name}", "build-bot")
                        .contentType("application/json")
                        .content("{\"message\": \"away\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("build-bot"))
                .andExpect(jsonPath("$.message").value("away"))
                .andExpect(jsonPath("$.updatedAt").exists());
    }

    @Test
    void getStatus_AfterSet_ReturnsSameMessage() throws Exception {
        String name = "get-after-set";
        mockMvc.perform(put("/api/v1/statuses/{name}", name)
                .contentType("application/json")
                .content("{\"message\": \"away\"}"));

        mockMvc.perform(get("/api/v1/statuses/{name}", name))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(name))
                .andExpect(jsonPath("$.message").value("away"));
    }

    @Test
    void setStatus_WhenOverwriting_ReturnsNewTrimmedMessageOnSubsequentGet() throws Exception {
        String name = "overwrite-status";
        mockMvc.perform(put("/api/v1/statuses/{name}", name)
                .contentType("application/json")
                .content("{\"message\": \"away\"}"));

        mockMvc.perform(put("/api/v1/statuses/{name}", name)
                        .contentType("application/json")
                        .content("{\"message\": \"  back  \"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("back"));

        mockMvc.perform(get("/api/v1/statuses/{name}", name))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("back"));
    }

    @Test
    void getStatus_WhenNameUnknown_ReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/statuses/{name}", "never-set"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("not_found"))
                .andExpect(jsonPath("$.details[0].field").value("name"))
                .andExpect(jsonPath("$.details[0].message").value("status does not exist"));
    }

    @Test
    void setStatus_WhenNameIsUppercase_ReturnsValidationFailed() throws Exception {
        mockMvc.perform(put("/api/v1/statuses/{name}", "Build-Bot")
                        .contentType("application/json")
                        .content("{\"message\": \"away\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("validation_failed"))
                .andExpect(jsonPath("$.details[0].field").value("name"));
    }

    @Test
    void setStatus_WhenNameStartsWithHyphen_ReturnsValidationFailed() throws Exception {
        mockMvc.perform(put("/api/v1/statuses/{name}", "-build-bot")
                        .contentType("application/json")
                        .content("{\"message\": \"away\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("validation_failed"))
                .andExpect(jsonPath("$.details[0].field").value("name"));
    }

    @Test
    void setStatus_WhenNameHasConsecutiveHyphens_ReturnsValidationFailed() throws Exception {
        mockMvc.perform(put("/api/v1/statuses/{name}", "build--bot")
                        .contentType("application/json")
                        .content("{\"message\": \"away\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("validation_failed"))
                .andExpect(jsonPath("$.details[0].field").value("name"));
    }

    @Test
    void setStatus_WhenNameExceeds40Characters_ReturnsValidationFailed() throws Exception {
        String tooLongName = "a".repeat(41);

        mockMvc.perform(put("/api/v1/statuses/{name}", tooLongName)
                        .contentType("application/json")
                        .content("{\"message\": \"away\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("validation_failed"))
                .andExpect(jsonPath("$.details[0].field").value("name"));
    }

    @Test
    void getStatus_WhenNameInvalid_ReturnsValidationFailed() throws Exception {
        mockMvc.perform(get("/api/v1/statuses/{name}", "Invalid_Name"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("validation_failed"))
                .andExpect(jsonPath("$.details[0].field").value("name"));
    }

    @Test
    void setStatus_WhenMessageIsMissing_ReturnsValidationFailed() throws Exception {
        mockMvc.perform(put("/api/v1/statuses/{name}", "build-bot")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("validation_failed"))
                .andExpect(jsonPath("$.details[0].field").value("message"))
                .andExpect(jsonPath("$.details[0].message").value("must not be blank"));
    }

    @Test
    void setStatus_WhenMessageIsBlank_ReturnsValidationFailed() throws Exception {
        mockMvc.perform(put("/api/v1/statuses/{name}", "build-bot")
                        .contentType("application/json")
                        .content("{\"message\": \"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("validation_failed"))
                .andExpect(jsonPath("$.details[0].field").value("message"))
                .andExpect(jsonPath("$.details[0].message").value("must not be blank"));
    }

    @Test
    void setStatus_WhenTrimmedMessageExceeds80Characters_ReturnsValidationFailed() throws Exception {
        String tooLong = "a".repeat(81);

        mockMvc.perform(put("/api/v1/statuses/{name}", "build-bot")
                        .contentType("application/json")
                        .content("{\"message\": \"" + tooLong + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("validation_failed"))
                .andExpect(jsonPath("$.details[0].field").value("message"))
                .andExpect(jsonPath("$.details[0].message").value("must be at most 80 characters"));
    }

    @Test
    void setStatus_WhenRawMessageIs82CharsButTrimsTo80_Succeeds() throws Exception {
        String trimsTo80 = " " + "a".repeat(80) + " ";

        mockMvc.perform(put("/api/v1/statuses/{name}", "build-bot")
                        .contentType("application/json")
                        .content("{\"message\": \"" + trimsTo80 + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", org.hamcrest.Matchers.hasLength(80)));
    }
}
