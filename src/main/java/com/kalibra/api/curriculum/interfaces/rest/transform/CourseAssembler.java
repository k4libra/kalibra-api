package com.kalibra.api.curriculum.interfaces.rest.transform;

import com.kalibra.api.curriculum.domain.model.aggregates.Course;
import com.kalibra.api.curriculum.domain.model.commands.CreateCourseCommand;
import com.kalibra.api.curriculum.domain.model.entities.Subtopic;
import com.kalibra.api.curriculum.domain.model.valueobjects.CourseCode;
import com.kalibra.api.curriculum.domain.model.valueobjects.CourseId;
import com.kalibra.api.curriculum.domain.model.valueobjects.SubtopicId;
import com.kalibra.api.curriculum.interfaces.rest.resources.CourseResource;
import com.kalibra.api.curriculum.interfaces.rest.resources.CreateCourseResource;
import com.kalibra.api.curriculum.interfaces.rest.resources.SubtopicResource;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.UUID;

@Mapper(componentModel = "spring")
public interface CourseAssembler {

    @Mapping(target = "holderId", source = "holderId")
    @Mapping(target = "name", source = "resource.name")
    @Mapping(target = "code", source = "resource.code")
    @Mapping(target = "subtopicNames", source = "resource.subtopicNames")
    CreateCourseCommand toCommand(CreateCourseResource resource, String holderId);

    CourseResource toResource(Course course);

    SubtopicResource toResource(Subtopic subtopic);

    // required by MapStruct: single-field VOs need an explicit converter.
    default CourseCode toCourseCode(String value) {
        return value == null ? null : new CourseCode(value);
    }

    default String map(CourseCode code) {
        return code == null ? null : code.value();
    }

    default UUID map(CourseId courseId) {
        return courseId == null ? null : courseId.value();
    }

    default UUID map(SubtopicId subtopicId) {
        return subtopicId == null ? null : subtopicId.value();
    }
}
