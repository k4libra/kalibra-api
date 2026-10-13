package com.kalibra.api.curriculum.domain.model.commands;

import com.kalibra.api.curriculum.domain.model.valueobjects.CourseId;

public record AddSubtopicCommand(CourseId courseId, String holderId, String name) { }
