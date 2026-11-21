package com.kalibra.api.curriculum.infrastructure.persistence.transform;

import com.kalibra.api.curriculum.domain.model.aggregates.TeacherWorkspace;
import com.kalibra.api.curriculum.domain.model.valueobjects.CourseId;
import com.kalibra.api.curriculum.infrastructure.persistence.entities.TeacherWorkspaceJpaEntity;
import org.mapstruct.Mapper;

import java.util.Optional;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface TeacherWorkspaceJpaMapper {

    TeacherWorkspaceJpaEntity toEntity(TeacherWorkspace teacherWorkspace);

    TeacherWorkspace toDomain(TeacherWorkspaceJpaEntity entity);

    // required by MapStruct 1.6: Optional fields need an explicit converter.
    default UUID map(Optional<CourseId> courseId) {
        return courseId == null ? null : courseId.map(CourseId::value).orElse(null);
    }

    default Optional<CourseId> toOptionalCourseId(UUID value) {
        return Optional.ofNullable(value).map(CourseId::new);
    }
}
