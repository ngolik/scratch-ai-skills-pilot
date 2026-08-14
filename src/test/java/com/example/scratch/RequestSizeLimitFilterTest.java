package com.example.scratch;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.example.scratch.farewell.FarewellController;
import com.example.scratch.farewell.FarewellService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FarewellController.class)
@Import(FarewellService.class)
class RequestSizeLimitFilterTest {

    private static final String FAREWELLS_PATH = "/api/v1/farewells";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createFarewell_WhenBodyExceedsSizeLimit_ReturnsPayloadTooLarge() throws Exception {
        String oversizedName = "A".repeat((int) RequestSizeLimitFilter.MAX_CONTENT_LENGTH_BYTES + 1);
        String oversizedBody = "{\"name\": \"" + oversizedName + "\"}";

        mockMvc.perform(post(FAREWELLS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(oversizedBody))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.error").value("payload_too_large"))
                .andExpect(jsonPath("$.details[0].field").value("body"));
    }

    @Test
    void createFarewell_WhenBodyWithinSizeLimit_IsNotRejectedByFilter() throws Exception {
        mockMvc.perform(post(FAREWELLS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Ada"}
                                """))
                .andExpect(status().isOk());
    }
}
