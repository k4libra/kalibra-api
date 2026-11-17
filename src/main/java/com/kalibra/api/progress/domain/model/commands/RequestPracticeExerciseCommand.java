package com.kalibra.api.progress.domain.model.commands;

import com.kalibra.api.progress.domain.model.valueobjects.CourseId;
import com.kalibra.api.progress.domain.model.valueobjects.PracticeMode;
import com.kalibra.api.progress.domain.model.valueobjects.SubtopicId;

public record RequestPracticeExerciseCommand(
        String holderId,
        CourseId courseId,
        SubtopicId subtopicId,
        PracticeMode mode
) {
    public RequestPracticeExerciseCommand(String holderId, CourseId courseId, SubtopicId subtopicId) {
        this(holderId, courseId, subtopicId, null);
    }
}
