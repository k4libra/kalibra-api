package com.kalibra.api.curriculum.domain.model.valueobjects;

import com.kalibra.api.curriculum.domain.exceptions.UnsupportedMaterialFormatException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ValueObjectsTest {

    @Test
    void shouldTrimCourseCodeAndRejectBlankOrTooLongOnes() {
        assertThat(new CourseCode("  MAT101 ").value()).isEqualTo("MAT101");
        assertThatThrownBy(() -> new CourseCode(" ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CourseCode("X".repeat(21))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldAcceptPaginationOnlyWithinBounds() {
        assertThat(Pagination.of(0, 100)).isEqualTo(new Pagination(0, 100));
        assertThatThrownBy(() -> Pagination.of(-1, 10)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Pagination.of(0, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Pagination.of(0, 101)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectEmptyMaterialFile() {
        assertThatThrownBy(() -> new MaterialFile("unit.pdf", MaterialFormat.PDF, "ref", 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldParseSupportedFormatsIgnoringCase() {
        assertThat(MaterialFormat.from("pdf")).isEqualTo(MaterialFormat.PDF);
        assertThat(MaterialFormat.from(" JPEG ")).isEqualTo(MaterialFormat.JPEG);
        assertThatThrownBy(() -> MaterialFormat.from("DOCX")).isInstanceOf(UnsupportedMaterialFormatException.class);
        assertThatThrownBy(() -> MaterialFormat.from(null)).isInstanceOf(UnsupportedMaterialFormatException.class);
    }
}
