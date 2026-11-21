package com.kalibra.api.curriculum.infrastructure.persistence.transform;

import com.kalibra.api.curriculum.domain.model.aggregates.Course;
import com.kalibra.api.curriculum.domain.model.entities.Subtopic;
import com.kalibra.api.curriculum.domain.model.valueobjects.CourseCode;
import com.kalibra.api.curriculum.domain.model.valueobjects.CourseId;
import com.kalibra.api.curriculum.domain.model.valueobjects.SubtopicId;
import com.kalibra.api.curriculum.infrastructure.persistence.entities.CourseJpaEntity;
import com.kalibra.api.curriculum.infrastructure.persistence.entities.SubtopicEmbeddable;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.UUID;

@Mapper(componentModel = "spring")
public interface CourseJpaMapper {

    CourseJpaEntity toEntity(Course course);

    Course toDomain(CourseJpaEntity entity);

    @Mapping(target = "subtopicId", source = "id")
    SubtopicEmbeddable toEmbeddable(Subtopic subtopic);

    @Mapping(target = "id", source = "subtopicId")
    Subtopic toSubtopic(SubtopicEmbeddable embeddable);

    // required by MapStruct: single-field VOs need an explicit converter.
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

    default String map(CourseCode code) {
        return code == null ? null : code.value();
    }

    default CourseCode toCourseCode(String value) {
        return value == null ? null : new CourseCode(value);
    }
}
