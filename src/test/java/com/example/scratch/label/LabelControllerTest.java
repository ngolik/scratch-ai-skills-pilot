package com.example.scratch.label;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LabelController.class)
@Import(LabelService.class)
class LabelControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void setLabel_WhenNameIsNew_CreatesAndReturnsValue() throws Exception {
        mockMvc.perform(put("/api/v1/labels/{name}", "release-channel")
                        .contentType("application/json")
                        .content("{\"value\": \"beta\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("release-channel"))
                .andExpect(jsonPath("$.value").value("beta"))
                .andExpect(jsonPath("$.updatedAt").exists());
    }

    @Test
    void getLabel_AfterSet_ReturnsSameValue() throws Exception {
        String name = "get-after-set";
        mockMvc.perform(put("/api/v1/labels/{name}", name)
                .contentType("application/json")
                .content("{\"value\": \"beta\"}"));

        mockMvc.perform(get("/api/v1/labels/{name}", name))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(name))
                .andExpect(jsonPath("$.value").value("beta"));
    }

    @Test
    void setLabel_WhenOverwriting_ReturnsNewTrimmedValueOnSubsequentGet() throws Exception {
        String name = "overwrite-label";
        mockMvc.perform(put("/api/v1/labels/{name}", name)
                .contentType("application/json")
                .content("{\"value\": \"beta\"}"));

        mockMvc.perform(put("/api/v1/labels/{name}", name)
                        .contentType("application/json")
                        .content("{\"value\": \"  stable  \"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.value").value("stable"));

        mockMvc.perform(get("/api/v1/labels/{name}", name))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.value").value("stable"));
    }

    @Test
    void getLabel_WhenNameUnknown_ReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/labels/{name}", "never-set"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("not_found"))
                .andExpect(jsonPath("$.details[0].field").value("name"))
                .andExpect(jsonPath("$.details[0].message").value("label does not exist"));
    }

    @Test
    void setLabel_WhenNameIsUppercase_ReturnsValidationFailed() throws Exception {
        mockMvc.perform(put("/api/v1/labels/{name}", "Release-Channel")
                        .contentType("application/json")
                        .content("{\"value\": \"beta\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("validation_failed"))
                .andExpect(jsonPath("$.details[0].field").value("name"));
    }

    @Test
    void setLabel_WhenNameStartsWithHyphen_ReturnsValidationFailed() throws Exception {
        mockMvc.perform(put("/api/v1/labels/{name}", "-release-channel")
                        .contentType("application/json")
                        .content("{\"value\": \"beta\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("validation_failed"))
                .andExpect(jsonPath("$.details[0].field").value("name"));
    }

    @Test
    void setLabel_WhenNameEndsWithHyphen_ReturnsValidationFailed() throws Exception {
        mockMvc.perform(put("/api/v1/labels/{name}", "release-channel-")
                        .contentType("application/json")
                        .content("{\"value\": \"beta\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("validation_failed"))
                .andExpect(jsonPath("$.details[0].field").value("name"));
    }

    @Test
    void setLabel_WhenNameHasConsecutiveHyphens_ReturnsValidationFailed() throws Exception {
        mockMvc.perform(put("/api/v1/labels/{name}", "release--channel")
                        .contentType("application/json")
                        .content("{\"value\": \"beta\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("validation_failed"))
                .andExpect(jsonPath("$.details[0].field").value("name"));
    }

    @Test
    void setLabel_WhenNameExceeds40Characters_ReturnsValidationFailed() throws Exception {
        String tooLongName = "a".repeat(41);

        mockMvc.perform(put("/api/v1/labels/{name}", tooLongName)
                        .contentType("application/json")
                        .content("{\"value\": \"beta\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("validation_failed"))
                .andExpect(jsonPath("$.details[0].field").value("name"));
    }

    @Test
    void getLabel_WhenNameInvalid_ReturnsValidationFailed() throws Exception {
        mockMvc.perform(get("/api/v1/labels/{name}", "Invalid_Name"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("validation_failed"))
                .andExpect(jsonPath("$.details[0].field").value("name"));
    }

    @Test
    void setLabel_WhenValueIsMissing_ReturnsValidationFailed() throws Exception {
        mockMvc.perform(put("/api/v1/labels/{name}", "release-channel")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("validation_failed"))
                .andExpect(jsonPath("$.details[0].field").value("value"))
                .andExpect(jsonPath("$.details[0].message").value("must not be blank"));
    }

    @Test
    void setLabel_WhenValueIsBlank_ReturnsValidationFailed() throws Exception {
        mockMvc.perform(put("/api/v1/labels/{name}", "release-channel")
                        .contentType("application/json")
                        .content("{\"value\": \"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("validation_failed"))
                .andExpect(jsonPath("$.details[0].field").value("value"))
                .andExpect(jsonPath("$.details[0].message").value("must not be blank"));
    }

    @Test
    void setLabel_WhenTrimmedValueExceeds32Characters_ReturnsValidationFailed() throws Exception {
        String tooLong = "a".repeat(33);

        mockMvc.perform(put("/api/v1/labels/{name}", "release-channel")
                        .contentType("application/json")
                        .content("{\"value\": \"" + tooLong + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("validation_failed"))
                .andExpect(jsonPath("$.details[0].field").value("value"))
                .andExpect(jsonPath("$.details[0].message").value("must be at most 32 characters"));
    }

    @Test
    void setLabel_WhenRawValueIs34CharsButTrimsTo32_Succeeds() throws Exception {
        String trimsTo32 = " " + "a".repeat(32) + " ";

        mockMvc.perform(put("/api/v1/labels/{name}", "release-channel")
                        .contentType("application/json")
                        .content("{\"value\": \"" + trimsTo32 + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.value", org.hamcrest.Matchers.hasLength(32)));
    }

    @Test
    void setLabel_WhenMissingAndBlankValue_ReturnIdenticalEnvelopeShape() throws Exception {
        mockMvc.perform(put("/api/v1/labels/{name}", "release-channel")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("validation_failed"))
                .andExpect(jsonPath("$.details[0].field").value("value"));

        mockMvc.perform(put("/api/v1/labels/{name}", "release-channel")
                        .contentType("application/json")
                        .content("{\"value\": \"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("validation_failed"))
                .andExpect(jsonPath("$.details[0].field").value("value"));
    }
}
