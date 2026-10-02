package com.kalibra.api.progress.infrastructure.persistence.transform;

import com.kalibra.api.progress.ProgressFixtures;
import com.kalibra.api.progress.domain.model.valueobjects.AnswerResult;
import com.kalibra.api.progress.domain.model.valueobjects.CourseId;
import com.kalibra.api.progress.domain.model.valueobjects.MasteryLevel;
import com.kalibra.api.progress.domain.model.valueobjects.MasteryProbability;
import com.kalibra.api.progress.domain.model.valueobjects.SubtopicId;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ProgressJpaMappersTest {

    private final ExerciseAttemptJpaMapper attemptMapper = new ExerciseAttemptJpaMapperImpl();
    private final SubtopicMasteryJpaMapper masteryMapper = new SubtopicMasteryJpaMapperImpl();

    private final String holderId = UUID.randomUUID().toString();
    private final CourseId courseId = new CourseId(UUID.randomUUID());
    private final SubtopicId subtopicId = new SubtopicId(UUID.randomUUID());

    @Test
    void shouldKeepAnAttemptWithItsMasteryChangeThroughPersistence() {
        var attempt = ProgressFixtures.attempt(holderId, courseId, subtopicId, "B", 0.30, 0.52);

        var entity = attemptMapper.toEntity(attempt);
        var restored = attemptMapper.toDomain(entity);

        assertThat(entity.getResult()).isEqualTo("CORRECT");
        assertThat(entity.getFeedbackExplanation()).isEqualTo(ProgressFixtures.EXPLANATION);
        assertThat(entity.getMasteryChange().getPreviousProbability()).isEqualTo(0.30);
        assertThat(entity.getMasteryChange().getCurrentProbability()).isEqualTo(0.52);
        assertThat(restored.getId()).isEqualTo(attempt.getId());
        assertThat(restored.getResult()).isEqualTo(AnswerResult.CORRECT);
        assertThat(restored.getFeedback()).isEqualTo(attempt.getFeedback());
        assertThat(restored.getMasteryChange()).isEqualTo(attempt.getMasteryChange());
        assertThat(restored.getSubtopicId()).isEqualTo(subtopicId);
        assertThat(restored.getAnsweredAt()).isEqualTo(attempt.getAnsweredAt());
    }

    @Test
    void shouldKeepAnAttemptWithoutPreviousMastery() {
        var attempt = ProgressFixtures.attempt(holderId, courseId, subtopicId, "A", null, 0.21);

        var entity = attemptMapper.toEntity(attempt);
        var restored = attemptMapper.toDomain(entity);

        assertThat(entity.getMasteryChange().getPreviousProbability()).isNull();
        assertThat(restored.getMasteryChange().previous()).isEmpty();
        assertThat(restored.getMasteryChange().current()).isEqualTo(new MasteryProbability(0.21));
    }

    @Test
    void shouldKeepAMasteryWithItsBaselineThroughPersistence() {
        var mastery = ProgressFixtures.mastery(holderId, courseId, subtopicId, 0.30, 0.74, MasteryLevel.HIGH);

        var entity = masteryMapper.toEntity(mastery);
        var restored = masteryMapper.toDomain(entity);

        assertThat(entity.getLevel()).isEqualTo("HIGH");
        assertThat(entity.getInitialEstimate()).isEqualTo(0.30);
        assertThat(restored.getId()).isEqualTo(mastery.getId());
        assertThat(restored.getInitialEstimate()).isEqualTo(new MasteryProbability(0.30));
        assertThat(restored.getCurrentEstimate()).isEqualTo(new MasteryProbability(0.74));
        assertThat(restored.getLevel()).isEqualTo(MasteryLevel.HIGH);
        assertThat(restored.getEstimatesCount()).isEqualTo(2);
        assertThat(restored.evolutionPoints()).isEqualTo(44);
    }
}
