package com.kalibra.api.curriculum.interfaces.rest.transform;

import com.kalibra.api.curriculum.domain.model.aggregates.Course;
import com.kalibra.api.curriculum.domain.model.commands.AddSubtopicCommand;
import com.kalibra.api.curriculum.domain.model.commands.CreateCourseCommand;
import com.kalibra.api.curriculum.domain.model.commands.RemoveSubtopicCommand;
import com.kalibra.api.curriculum.domain.model.commands.UpdateCourseCommand;
import com.kalibra.api.curriculum.domain.model.commands.UpdateSubtopicCommand;
import com.kalibra.api.curriculum.domain.model.entities.Subtopic;
import com.kalibra.api.curriculum.domain.model.valueobjects.CourseCode;
import com.kalibra.api.curriculum.domain.model.valueobjects.CourseId;
import com.kalibra.api.curriculum.domain.model.valueobjects.SubtopicId;
import com.kalibra.api.curriculum.domain.model.valueobjects.SubtopicStatus;
import com.kalibra.api.curriculum.interfaces.rest.resources.AddSubtopicResource;
import com.kalibra.api.curriculum.interfaces.rest.resources.CourseResource;
import com.kalibra.api.curriculum.interfaces.rest.resources.CreateCourseResource;
import com.kalibra.api.curriculum.interfaces.rest.resources.SubtopicResource;
import com.kalibra.api.curriculum.interfaces.rest.resources.UpdateCourseResource;
import com.kalibra.api.curriculum.interfaces.rest.resources.UpdateSubtopicResource;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface CourseAssembler {

    @Mapping(target = "holderId", source = "holderId")
    @Mapping(target = "name", source = "resource.name")
    @Mapping(target = "code", source = "resource.code")
    @Mapping(target = "subtopicNames", source = "resource.subtopicNames")
    CreateCourseCommand toCommand(CreateCourseResource resource, String holderId);

    UpdateCourseCommand toCommand(UpdateCourseResource resource, CourseId courseId, String holderId);

    AddSubtopicCommand toCommand(AddSubtopicResource resource, CourseId courseId, String holderId);

    UpdateSubtopicCommand toCommand(UpdateSubtopicResource resource, CourseId courseId, SubtopicId subtopicId, String holderId);

    RemoveSubtopicCommand toCommand(CourseId courseId, SubtopicId subtopicId, String holderId);

    CourseResource toResource(Course course, @Context Set<SubtopicId> lockedSubtopicIds);

    @Mapping(target = "locked", expression = "java(lockedSubtopicIds.contains(subtopic.getId()))")
    SubtopicResource toResource(Subtopic subtopic, @Context Set<SubtopicId> lockedSubtopicIds);

    default Optional<String> toOptionalString(String value) {
        return Optional.ofNullable(value);
    }

    default Optional<CourseCode> toOptionalCode(String value) {
        return Optional.ofNullable(value).map(CourseCode::new);
    }

    default Optional<Integer> toOptionalOrder(Integer value) {
        return Optional.ofNullable(value);
    }

    default Optional<SubtopicStatus> toOptionalStatus(SubtopicStatus value) {
        return Optional.ofNullable(value);
    }

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
