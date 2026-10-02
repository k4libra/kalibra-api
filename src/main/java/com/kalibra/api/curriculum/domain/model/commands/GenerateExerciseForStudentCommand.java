package com.kalibra.api.curriculum.domain.model.commands;

import com.kalibra.api.curriculum.domain.model.valueobjects.CourseId;
import com.kalibra.api.curriculum.domain.model.valueobjects.MasteryProbability;
import com.kalibra.api.curriculum.domain.model.valueobjects.SubtopicId;

import java.util.Optional;

public record GenerateExerciseForStudentCommand(CourseId courseId, SubtopicId subtopicId, Optional<MasteryProbability> mastery) { }
