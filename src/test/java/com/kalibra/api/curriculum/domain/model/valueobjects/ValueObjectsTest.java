package com.kalibra.api.curriculum.domain.model.valueobjects;

import com.kalibra.api.curriculum.domain.exceptions.UnsupportedMaterialFormatException;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

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

    @Test
    void shouldRecognizeAFileByTheSignatureOfItsDeclaredFormat() {
        var pdf = "%PDF-1.7 content".getBytes(StandardCharsets.US_ASCII);
        var png = new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0};
        var jpeg = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0};

        assertThat(MaterialFormat.PDF.isSignatureOf(pdf)).isTrue();
        assertThat(MaterialFormat.PNG.isSignatureOf(png)).isTrue();
        assertThat(MaterialFormat.JPEG.isSignatureOf(jpeg)).isTrue();
    }

    @Test
    void shouldTreatAMislabeledTruncatedOrMissingFileAsCorrupt() {
        var pdf = "%PDF-1.7 content".getBytes(StandardCharsets.US_ASCII);

        assertThat(MaterialFormat.PNG.isSignatureOf(pdf)).isFalse();
        assertThat(MaterialFormat.JPEG.isSignatureOf(pdf)).isFalse();
        assertThat(MaterialFormat.PDF.isSignatureOf(new byte[]{1, 2, 3})).isFalse();
        assertThat(MaterialFormat.PDF.isSignatureOf(new byte[0])).isFalse();
        assertThat(MaterialFormat.PDF.isSignatureOf(null)).isFalse();
        assertThat(UnsupportedMaterialFormatException.unreadable("PDF")).hasMessageContaining("corrupt");
    }
}
