package com.kalibra.api.curriculum.infrastructure.persistence.transform;

import com.kalibra.api.curriculum.GeneratedExerciseFixtures;
import com.kalibra.api.curriculum.domain.model.aggregates.Course;
import com.kalibra.api.curriculum.domain.model.commands.CreateCourseCommand;
import com.kalibra.api.curriculum.domain.model.valueobjects.CourseCode;
import com.kalibra.api.curriculum.domain.model.valueobjects.VerificationVerdict;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GeneratedExerciseJpaMapperTest {

    private final GeneratedExerciseJpaMapper mapper = new GeneratedExerciseJpaMapperImpl();

    private final Course course = Course.create(new CreateCourseCommand("teacher-1", "Algebra",
            new CourseCode("MAT101"), List.of("Equations")));

    @Test
    void shouldKeepADiscardedExerciseWithItsRejectionReasonThroughPersistence() {
        var discarded = GeneratedExerciseFixtures.discarded(course, course.getSubtopics().getFirst().getId());

        var entity = mapper.toEntity(discarded);
        var restored = mapper.toDomain(entity);

        assertThat(entity.getVerification().getVerdict()).isEqualTo("DISCARDED");
        assertThat(entity.getVerification().getRejectionReason()).isEqualTo(GeneratedExerciseFixtures.REJECTION_REASON);
        assertThat(entity.getOptions()).extracting("optionKey").containsExactly("A", "B", "C", "D");
        assertThat(restored.getId()).isEqualTo(discarded.getId());
        assertThat(restored.getVerification().verdict()).isEqualTo(VerificationVerdict.DISCARDED);
        assertThat(restored.getVerification().rejectionReason()).contains(GeneratedExerciseFixtures.REJECTION_REASON);
        assertThat(restored.getVerification().usedFallback()).isTrue();
        assertThat(restored.isAvailableToStudents()).isFalse();
    }

    @Test
    void shouldKeepAnApprovedExerciseWithItsOptionsThroughPersistence() {
        var approved = GeneratedExerciseFixtures.approved(course, course.getSubtopics().getFirst().getId());

        var restored = mapper.toDomain(mapper.toEntity(approved));

        assertThat(restored.getVerification().rejectionReason()).isEmpty();
        assertThat(restored.isAvailableToStudents()).isTrue();
        assertThat(restored.correctOption().getKey()).isEqualTo("B");
        assertThat(restored.getOptions()).hasSize(4);
        assertThat(restored.getDifficulty()).isEqualTo(approved.getDifficulty());
        assertThat(restored.getOrigin()).isEqualTo(approved.getOrigin());
    }
}
