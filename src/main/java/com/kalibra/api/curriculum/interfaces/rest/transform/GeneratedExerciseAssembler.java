package com.kalibra.api.curriculum.interfaces.rest.transform;

import com.kalibra.api.curriculum.domain.model.aggregates.GeneratedExercise;
import com.kalibra.api.curriculum.domain.model.commands.GenerateExercisesForSubtopicCommand;
import com.kalibra.api.curriculum.domain.model.entities.ExerciseOption;
import com.kalibra.api.curriculum.domain.model.valueobjects.CourseExerciseCatalog;
import com.kalibra.api.curriculum.domain.model.valueobjects.CourseId;
import com.kalibra.api.curriculum.domain.model.valueobjects.ExerciseId;
import com.kalibra.api.curriculum.domain.model.valueobjects.GeneratedExercisePage;
import com.kalibra.api.curriculum.domain.model.valueobjects.SubtopicExerciseCount;
import com.kalibra.api.curriculum.domain.model.valueobjects.SubtopicId;
import com.kalibra.api.curriculum.interfaces.rest.resources.CourseExerciseCatalogResource;
import com.kalibra.api.curriculum.interfaces.rest.resources.ExerciseOptionResource;
import com.kalibra.api.curriculum.interfaces.rest.resources.GenerateExerciseResource;
import com.kalibra.api.curriculum.interfaces.rest.resources.GeneratedExercisePageResource;
import com.kalibra.api.curriculum.interfaces.rest.resources.GeneratedExerciseResource;
import com.kalibra.api.curriculum.interfaces.rest.resources.SubtopicExerciseCountResource;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Optional;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface GeneratedExerciseAssembler {

    @Mapping(target = "holderId", source = "holderId")
    @Mapping(target = "courseId", source = "courseId")
    @Mapping(target = "subtopicId", source = "resource.subtopicId")
    @Mapping(target = "quantity", source = "resource.quantity")
    GenerateExercisesForSubtopicCommand toCommand(GenerateExerciseResource resource, UUID courseId, String holderId);

    @Mapping(target = "verdict", source = "verification.verdict")
    @Mapping(target = "rejectionReason", source = "verification.rejectionReason")
    GeneratedExerciseResource toResource(GeneratedExercise exercise);

    ExerciseOptionResource toResource(ExerciseOption option);

    @Mapping(target = "content", source = "items")
    GeneratedExercisePageResource toResource(GeneratedExercisePage page);

    CourseExerciseCatalogResource toResource(CourseExerciseCatalog catalog);

    SubtopicExerciseCountResource toResource(SubtopicExerciseCount count);

    // required by MapStruct: VOs and Optional fields need explicit converters.
    default CourseId toCourseId(UUID value) {
        return value == null ? null : new CourseId(value);
    }

    default SubtopicId toSubtopicId(UUID value) {
        return value == null ? null : new SubtopicId(value);
    }

    default UUID map(ExerciseId exerciseId) {
        return exerciseId == null ? null : exerciseId.value();
    }

    default UUID map(SubtopicId subtopicId) {
        return subtopicId == null ? null : subtopicId.value();
    }

    default String map(Optional<String> value) {
        return value == null ? null : value.orElse(null);
    }
}
