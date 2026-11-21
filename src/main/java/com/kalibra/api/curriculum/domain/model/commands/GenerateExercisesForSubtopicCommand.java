package com.kalibra.api.curriculum.domain.model.commands;

import com.kalibra.api.curriculum.domain.model.valueobjects.CourseId;
import com.kalibra.api.curriculum.domain.model.valueobjects.SubtopicId;

public record GenerateExercisesForSubtopicCommand(String holderId, CourseId courseId, SubtopicId subtopicId, int quantity) { }
