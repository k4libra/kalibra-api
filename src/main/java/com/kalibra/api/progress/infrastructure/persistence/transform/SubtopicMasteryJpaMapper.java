package com.kalibra.api.progress.infrastructure.persistence.transform;

import com.kalibra.api.progress.domain.model.aggregates.SubtopicMastery;
import com.kalibra.api.progress.domain.model.valueobjects.CourseId;
import com.kalibra.api.progress.domain.model.valueobjects.MasteryProbability;
import com.kalibra.api.progress.domain.model.valueobjects.SubtopicId;
import com.kalibra.api.progress.domain.model.valueobjects.SubtopicMasteryId;
import com.kalibra.api.progress.infrastructure.persistence.entities.SubtopicMasteryJpaEntity;
import org.mapstruct.Mapper;

import java.util.UUID;

@Mapper(componentModel = "spring")
public interface SubtopicMasteryJpaMapper {

    SubtopicMasteryJpaEntity toEntity(SubtopicMastery subtopicMastery);

    SubtopicMastery toDomain(SubtopicMasteryJpaEntity entity);

    // required by MapStruct 1.6: single-field VOs need explicit converters.
    default UUID map(SubtopicMasteryId subtopicMasteryId) {
        return subtopicMasteryId == null ? null : subtopicMasteryId.value();
    }

    default SubtopicMasteryId toSubtopicMasteryId(UUID value) {
        return value == null ? null : new SubtopicMasteryId(value);
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

    default double map(MasteryProbability probability) {
        return probability == null ? 0 : probability.value();
    }

    default MasteryProbability toMasteryProbability(double value) {
        return new MasteryProbability(value);
    }
}
