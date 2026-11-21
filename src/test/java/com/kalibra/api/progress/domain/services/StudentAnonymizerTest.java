package com.kalibra.api.progress.domain.services;

import com.kalibra.api.progress.domain.model.valueobjects.StudentId;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class StudentAnonymizerTest {

    private final StudentAnonymizer anonymizer = new StudentAnonymizer();

    @Test
    void shouldGiveTheSameStudentTheSameCodeEveryTime() {
        var student = new StudentId(UUID.randomUUID());

        assertThat(anonymizer.codeFor(student)).isEqualTo(anonymizer.codeFor(student));
    }

    @Test
    void shouldGiveDifferentStudentsDifferentCodes() {
        assertThat(anonymizer.codeFor(new StudentId(UUID.randomUUID())))
                .isNotEqualTo(anonymizer.codeFor(new StudentId(UUID.randomUUID())));
    }

    @Test
    void shouldNotRevealTheStudentIdInTheCode() {
        var student = new StudentId(UUID.randomUUID());

        var code = anonymizer.codeFor(student).value();

        assertThat(code).matches("STU-[0-9A-F]{10}");
        assertThat(code.toLowerCase()).doesNotContain(student.value().toString().substring(0, 8));
    }
}
