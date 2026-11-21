package com.kalibra.api.curriculum.infrastructure.persistence.transform;

import com.kalibra.api.curriculum.domain.model.aggregates.GeneratedExercise;
import com.kalibra.api.curriculum.domain.model.entities.ExerciseOption;
import com.kalibra.api.curriculum.domain.model.valueobjects.CourseId;
import com.kalibra.api.curriculum.domain.model.valueobjects.ExerciseId;
import com.kalibra.api.curriculum.domain.model.valueobjects.SubtopicId;
import com.kalibra.api.curriculum.domain.model.valueobjects.VerificationOutcome;
import com.kalibra.api.curriculum.infrastructure.persistence.entities.ExerciseOptionEmbeddable;
import com.kalibra.api.curriculum.infrastructure.persistence.entities.GeneratedExerciseJpaEntity;
import com.kalibra.api.curriculum.infrastructure.persistence.entities.VerificationOutcomeEmbeddable;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Optional;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface GeneratedExerciseJpaMapper {

    GeneratedExerciseJpaEntity toEntity(GeneratedExercise exercise);

    GeneratedExercise toDomain(GeneratedExerciseJpaEntity entity);

    @Mapping(target = "optionKey", source = "key")
    ExerciseOptionEmbeddable toEmbeddable(ExerciseOption option);

    @Mapping(target = "key", source = "optionKey")
    ExerciseOption toExerciseOption(ExerciseOptionEmbeddable embeddable);

    VerificationOutcomeEmbeddable toEmbeddable(VerificationOutcome verification);

    VerificationOutcome toVerificationOutcome(VerificationOutcomeEmbeddable embeddable);

    // required by MapStruct 1.6: Optional fields and single-field VOs need explicit converters.
    default String fromOptionalReason(Optional<String> reason) {
        return reason == null ? null : reason.orElse(null);
    }

    default Optional<String> toOptionalReason(String reason) {
        return Optional.ofNullable(reason);
    }

    default UUID map(ExerciseId exerciseId) {
        return exerciseId == null ? null : exerciseId.value();
    }

    default ExerciseId toExerciseId(UUID value) {
        return value == null ? null : new ExerciseId(value);
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
}
