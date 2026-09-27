package com.kalibra.api.curriculum.interfaces.rest.transform;

import com.kalibra.api.curriculum.domain.model.aggregates.TeacherWorkspace;
import com.kalibra.api.curriculum.domain.model.commands.SelectActiveCourseCommand;
import com.kalibra.api.curriculum.domain.model.valueobjects.CourseId;
import com.kalibra.api.curriculum.interfaces.rest.resources.SelectActiveCourseResource;
import com.kalibra.api.curriculum.interfaces.rest.resources.TeacherWorkspaceResource;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Optional;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface TeacherWorkspaceAssembler {

    @Mapping(target = "holderId", source = "holderId")
    @Mapping(target = "courseId", source = "resource.courseId")
    SelectActiveCourseCommand toCommand(SelectActiveCourseResource resource, String holderId);

    TeacherWorkspaceResource toResource(TeacherWorkspace workspace);

    // required by MapStruct: VOs and Optional fields need explicit converters.
    default CourseId toCourseId(UUID value) {
        return value == null ? null : new CourseId(value);
    }

    default UUID map(Optional<CourseId> courseId) {
        return courseId == null ? null : courseId.map(CourseId::value).orElse(null);
    }
}
