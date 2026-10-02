package com.kalibra.api.progress.domain.model.commands;

import com.kalibra.api.progress.domain.model.valueobjects.CourseId;
import com.kalibra.api.progress.domain.model.valueobjects.ExerciseId;

public record SubmitAnswerCommand(
        String holderId,
        CourseId courseId,
        ExerciseId exerciseId,
        String selectedOptionKey
) {
}
