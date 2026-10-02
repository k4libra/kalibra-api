package com.kalibra.api.progress.interfaces.rest.transform;

import com.kalibra.api.progress.domain.model.aggregates.ExerciseAttempt;
import com.kalibra.api.progress.domain.model.commands.RequestPracticeExerciseCommand;
import com.kalibra.api.progress.domain.model.commands.SubmitAnswerCommand;
import com.kalibra.api.progress.domain.model.valueobjects.AttemptHistoryLine;
import com.kalibra.api.progress.domain.model.valueobjects.AttemptHistoryView;
import com.kalibra.api.progress.domain.model.valueobjects.AttemptId;
import com.kalibra.api.progress.domain.model.valueobjects.CourseId;
import com.kalibra.api.progress.domain.model.valueobjects.ExerciseId;
import com.kalibra.api.progress.domain.model.valueobjects.MasteryProbability;
import com.kalibra.api.progress.domain.model.valueobjects.PracticeExerciseView;
import com.kalibra.api.progress.domain.model.valueobjects.SubtopicId;
import com.kalibra.api.progress.interfaces.rest.resources.AttemptHistoryResource;
import com.kalibra.api.progress.interfaces.rest.resources.ExerciseAttemptResource;
import com.kalibra.api.progress.interfaces.rest.resources.PracticeExerciseResource;
import com.kalibra.api.progress.interfaces.rest.resources.RequestPracticeExerciseResource;
import com.kalibra.api.progress.interfaces.rest.resources.SubmitAnswerResource;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Optional;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface ExerciseAttemptAssembler {

    @Mapping(target = "holderId", source = "holderId")
    @Mapping(target = "courseId", source = "resource.courseId")
    @Mapping(target = "exerciseId", source = "resource.exerciseId")
    @Mapping(target = "selectedOptionKey", source = "resource.selectedOptionKey")
    SubmitAnswerCommand toCommand(SubmitAnswerResource resource, String holderId);

    @Mapping(target = "holderId", source = "holderId")
    @Mapping(target = "courseId", source = "resource.courseId")
    @Mapping(target = "subtopicId", source = "resource.subtopicId")
    RequestPracticeExerciseCommand toCommand(RequestPracticeExerciseResource resource, String holderId);

    @Mapping(target = "statement", source = "exerciseStatement")
    @Mapping(target = "explanation", source = "feedback.explanation")
    @Mapping(target = "previousMastery", source = "masteryChange.previous")
    @Mapping(target = "currentMastery", source = "masteryChange.current")
    @Mapping(target = "masteryDeltaPoints", expression = "java(attempt.getMasteryChange().deltaPoints())")
    @Mapping(target = "masteryChanged", expression = "java(attempt.getMasteryChange().hasChanged())")
    ExerciseAttemptResource toResource(ExerciseAttempt attempt);

    @Mapping(target = "id", source = "attemptId")
    @Mapping(target = "explanation", source = "feedback.explanation")
    @Mapping(target = "previousMastery", source = "masteryChange.previous")
    @Mapping(target = "currentMastery", source = "masteryChange.current")
    @Mapping(target = "masteryDeltaPoints", expression = "java(line.masteryChange().deltaPoints())")
    @Mapping(target = "masteryChanged", expression = "java(line.masteryChange().hasChanged())")
    ExerciseAttemptResource toResource(AttemptHistoryLine line);

    @Mapping(target = "content", source = "lines")
    AttemptHistoryResource toResource(AttemptHistoryView view);

    PracticeExerciseResource toResource(PracticeExerciseView view);

    // required by MapStruct: VOs and Optional fields need explicit converters.
    default CourseId toCourseId(UUID value) {
        return value == null ? null : new CourseId(value);
    }

    default ExerciseId toExerciseId(UUID value) {
        return value == null ? null : new ExerciseId(value);
    }

    default SubtopicId toSubtopicId(UUID value) {
        return value == null ? null : new SubtopicId(value);
    }

    default UUID map(AttemptId attemptId) {
        return attemptId == null ? null : attemptId.value();
    }

    default UUID map(SubtopicId subtopicId) {
        return subtopicId == null ? null : subtopicId.value();
    }

    default Double toPercentage(Optional<MasteryProbability> probability) {
        return probability == null ? null : probability.map(this::toPercentage).orElse(null);
    }

    default Double toPercentage(MasteryProbability probability) {
        return probability == null ? null : (double) probability.asPercentage();
    }
}
