package com.kalibra.api.progress.infrastructure.persistence.transform;

import com.kalibra.api.progress.domain.model.aggregates.ExerciseAttempt;
import com.kalibra.api.progress.domain.model.valueobjects.AttemptId;
import com.kalibra.api.progress.domain.model.valueobjects.CourseId;
import com.kalibra.api.progress.domain.model.valueobjects.ExerciseId;
import com.kalibra.api.progress.domain.model.valueobjects.Feedback;
import com.kalibra.api.progress.domain.model.valueobjects.MasteryChange;
import com.kalibra.api.progress.domain.model.valueobjects.MasteryProbability;
import com.kalibra.api.progress.domain.model.valueobjects.SubtopicId;
import com.kalibra.api.progress.infrastructure.persistence.entities.ExerciseAttemptJpaEntity;
import com.kalibra.api.progress.infrastructure.persistence.entities.MasteryChangeEmbeddable;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Optional;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface ExerciseAttemptJpaMapper {

    @Mapping(target = "feedbackExplanation", source = "feedback")
    ExerciseAttemptJpaEntity toEntity(ExerciseAttempt attempt);

    @Mapping(target = "feedback", source = "feedbackExplanation")
    ExerciseAttempt toDomain(ExerciseAttemptJpaEntity entity);

    // required by MapStruct 1.6: Optional fields and single-field VOs need explicit converters.
    default MasteryChangeEmbeddable toEmbeddable(MasteryChange change) {
        if (change == null) {
            return null;
        }
        var embeddable = new MasteryChangeEmbeddable();
        embeddable.setPreviousProbability(change.previous().map(MasteryProbability::value).orElse(null));
        embeddable.setCurrentProbability(change.current().value());
        return embeddable;
    }

    default MasteryChange toMasteryChange(MasteryChangeEmbeddable embeddable) {
        if (embeddable == null) {
            return null;
        }
        return new MasteryChange(
                Optional.ofNullable(embeddable.getPreviousProbability()).map(MasteryProbability::new),
                new MasteryProbability(embeddable.getCurrentProbability())
        );
    }

    default String map(Feedback feedback) {
        return feedback == null ? null : feedback.explanation();
    }

    default Feedback toFeedback(String explanation) {
        return new Feedback(explanation);
    }

    default UUID map(AttemptId attemptId) {
        return attemptId == null ? null : attemptId.value();
    }

    default AttemptId toAttemptId(UUID value) {
        return value == null ? null : new AttemptId(value);
    }

    default UUID map(CourseId courseId) {
        return courseId == null ? null : courseId.value();
    }

    default CourseId toCourseId(UUID value) {
        return value == null ? null : new CourseId(value);
    }

    default UUID map(SubtopicId subtopicId) {
        return subtopicId == null ? null : subtopicId.value();
    }

    default SubtopicId toSubtopicId(UUID value) {
        return value == null ? null : new SubtopicId(value);
    }

    default UUID map(ExerciseId exerciseId) {
        return exerciseId == null ? null : exerciseId.value();
    }

    default ExerciseId toExerciseId(UUID value) {
        return value == null ? null : new ExerciseId(value);
    }
}
