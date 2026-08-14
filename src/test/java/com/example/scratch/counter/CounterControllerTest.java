package com.example.scratch.counter;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CounterController.class)
@Import(CounterService.class)
class CounterControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void incrementCounter_WhenNameIsNew_ReturnsValueOne() throws Exception {
        mockMvc.perform(post("/api/v1/counters/{name}/increments", "increment-new"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("increment-new"))
                .andExpect(jsonPath("$.value").value(1))
                .andExpect(jsonPath("$.updatedAt").exists());
    }

    @Test
    void incrementCounter_WhenCalledTwice_ReturnsValueTwo() throws Exception {
        String name = "increment-twice";

        mockMvc.perform(post("/api/v1/counters/{name}/increments", name))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.value").value(1));

        mockMvc.perform(post("/api/v1/counters/{name}/increments", name))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.value").value(2));
    }

    @Test
    void incrementCounter_WhenNameIsUppercase_ReturnsValidationFailed() throws Exception {
        mockMvc.perform(post("/api/v1/counters/{name}/increments", "Jobs"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("validation_failed"))
                .andExpect(jsonPath("$.details[0].field").value("name"));
    }

    @Test
    void incrementCounter_WhenNameStartsWithHyphen_ReturnsValidationFailed() throws Exception {
        mockMvc.perform(post("/api/v1/counters/{name}/increments", "-jobs"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("validation_failed"))
                .andExpect(jsonPath("$.details[0].field").value("name"));
    }

    @Test
    void incrementCounter_WhenNameEndsWithHyphen_ReturnsValidationFailed() throws Exception {
        mockMvc.perform(post("/api/v1/counters/{name}/increments", "jobs-"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("validation_failed"))
                .andExpect(jsonPath("$.details[0].field").value("name"));
    }

    @Test
    void incrementCounter_WhenNameHasConsecutiveHyphens_ReturnsValidationFailed() throws Exception {
        mockMvc.perform(post("/api/v1/counters/{name}/increments", "jo--bs"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("validation_failed"))
                .andExpect(jsonPath("$.details[0].field").value("name"));
    }

    @Test
    void incrementCounter_WhenNameExceeds40Characters_ReturnsValidationFailed() throws Exception {
        String tooLongName = "a".repeat(41);

        mockMvc.perform(post("/api/v1/counters/{name}/increments", tooLongName))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("validation_failed"))
                .andExpect(jsonPath("$.details[0].field").value("name"));
    }

    @Test
    void getCounter_AfterOneIncrement_ReturnsValueOne() throws Exception {
        String name = "get-after-one";
        mockMvc.perform(post("/api/v1/counters/{name}/increments", name));

        mockMvc.perform(get("/api/v1/counters/{name}", name))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(name))
                .andExpect(jsonPath("$.value").value(1));
    }

    @Test
    void getCounter_AfterTwoIncrements_ReturnsValueTwo() throws Exception {
        String name = "get-after-two";
        mockMvc.perform(post("/api/v1/counters/{name}/increments", name));
        mockMvc.perform(post("/api/v1/counters/{name}/increments", name));

        mockMvc.perform(get("/api/v1/counters/{name}", name))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.value").value(2));
    }

    @Test
    void getCounter_WhenNameUnknown_ReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/counters/{name}", "never-incremented"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("not_found"))
                .andExpect(jsonPath("$.details[0].field").value("name"))
                .andExpect(jsonPath("$.details[0].message").value("counter does not exist"));
    }

    @Test
    void getCounter_WhenNameInvalid_ReturnsValidationFailed() throws Exception {
        mockMvc.perform(get("/api/v1/counters/{name}", "Invalid_Name"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("validation_failed"))
                .andExpect(jsonPath("$.details[0].field").value("name"));
    }
}
