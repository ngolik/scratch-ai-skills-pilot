package com.example.scratch.label;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LabelServiceTest {

    private LabelService labelService;

    @BeforeEach
    void setUp() {
        labelService = new LabelService();
    }

    @Test
    void setLabel_WhenLabelIsNew_ReturnsValue() {
        LabelResponse response = labelService.setLabel("release-channel", "beta");

        assertThat(response.name()).isEqualTo("release-channel");
        assertThat(response.value()).isEqualTo("beta");
        assertThat(response.updatedAt()).isNotNull();
    }

    @Test
    void getLabel_AfterSetLabel_ReturnsSameValue() {
        labelService.setLabel("release-channel", "beta");

        LabelResponse response = labelService.getLabel("release-channel");

        assertThat(response.name()).isEqualTo("release-channel");
        assertThat(response.value()).isEqualTo("beta");
    }

    @Test
    void getLabel_AfterOverwrite_ReflectsNewTrimmedValue() {
        labelService.setLabel("release-channel", "beta");
        labelService.setLabel("release-channel", "  stable  ");

        LabelResponse response = labelService.getLabel("release-channel");

        assertThat(response.value()).isEqualTo("stable");
    }

    @Test
    void setLabel_WhenValueHasSurroundingWhitespace_StoresTrimmedValue() {
        LabelResponse response = labelService.setLabel("release-channel", "  beta  ");

        assertThat(response.value()).isEqualTo("beta");
    }

    @Test
    void getLabel_WhenLabelUnknown_ThrowsLabelNotFoundException() {
        assertThatThrownBy(() -> labelService.getLabel("missing"))
                .isInstanceOf(LabelNotFoundException.class);
    }

    @Test
    void setLabel_WhenValueIsBlank_ThrowsLabelValueInvalidException() {
        assertThatThrownBy(() -> labelService.setLabel("release-channel", "   "))
                .isInstanceOf(LabelValueInvalidException.class);
    }

    @Test
    void setLabel_WhenTrimmedValueExceeds32Characters_ThrowsLabelValueInvalidException() {
        String tooLong = "a".repeat(33);

        assertThatThrownBy(() -> labelService.setLabel("release-channel", tooLong))
                .isInstanceOf(LabelValueInvalidException.class);
    }

    @Test
    void setLabel_WhenRawValueIs34CharsButTrimsTo32_Succeeds() {
        String trimsTo32 = " " + "a".repeat(32) + " ";

        LabelResponse response = labelService.setLabel("release-channel", trimsTo32);

        assertThat(response.value()).hasSize(32);
    }
}
