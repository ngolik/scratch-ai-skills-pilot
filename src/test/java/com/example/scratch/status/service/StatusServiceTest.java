package com.example.scratch.status.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.scratch.status.dto.StatusResponse;
import com.example.scratch.status.repository.InMemoryStatusRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StatusServiceTest {

    private StatusService statusService;

    @BeforeEach
    void setUp() {
        statusService = new StatusService(new InMemoryStatusRepository());
    }

    @Test
    void setStatus_WhenStatusIsNew_ReturnsMessage() {
        StatusResponse response = statusService.setStatus("build-bot", "away");

        assertThat(response.name()).isEqualTo("build-bot");
        assertThat(response.message()).isEqualTo("away");
        assertThat(response.updatedAt()).isNotNull();
    }

    @Test
    void getStatus_AfterSetStatus_ReturnsSameMessage() {
        statusService.setStatus("build-bot", "away");

        StatusResponse response = statusService.getStatus("build-bot");

        assertThat(response.name()).isEqualTo("build-bot");
        assertThat(response.message()).isEqualTo("away");
    }

    @Test
    void getStatus_AfterOverwrite_ReflectsNewTrimmedMessage() {
        statusService.setStatus("build-bot", "away");
        statusService.setStatus("build-bot", "  back  ");

        StatusResponse response = statusService.getStatus("build-bot");

        assertThat(response.message()).isEqualTo("back");
    }

    @Test
    void setStatus_WhenMessageHasSurroundingWhitespace_StoresTrimmedValue() {
        StatusResponse response = statusService.setStatus("build-bot", "  away  ");

        assertThat(response.message()).isEqualTo("away");
    }

    @Test
    void getStatus_WhenStatusUnknown_ThrowsStatusNotFoundException() {
        assertThatThrownBy(() -> statusService.getStatus("missing"))
                .isInstanceOf(StatusNotFoundException.class);
    }

    @Test
    void setStatus_WhenMessageIsBlank_ThrowsStatusMessageInvalidException() {
        assertThatThrownBy(() -> statusService.setStatus("build-bot", "   "))
                .isInstanceOf(StatusMessageInvalidException.class);
    }

    @Test
    void setStatus_WhenTrimmedMessageExceeds80Characters_ThrowsStatusMessageInvalidException() {
        String tooLong = "a".repeat(81);

        assertThatThrownBy(() -> statusService.setStatus("build-bot", tooLong))
                .isInstanceOf(StatusMessageInvalidException.class);
    }

    @Test
    void setStatus_WhenRawMessageIs82CharsButTrimsTo80_Succeeds() {
        String trimsTo80 = " " + "a".repeat(80) + " ";

        StatusResponse response = statusService.setStatus("build-bot", trimsTo80);

        assertThat(response.message()).hasSize(80);
    }
}
