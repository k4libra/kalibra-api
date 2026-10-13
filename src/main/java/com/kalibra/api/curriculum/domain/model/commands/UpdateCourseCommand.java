package com.kalibra.api.curriculum.domain.model.commands;

import com.kalibra.api.curriculum.domain.model.valueobjects.CourseCode;
import com.kalibra.api.curriculum.domain.model.valueobjects.CourseId;

import java.util.Optional;

public record UpdateCourseCommand(CourseId courseId, String holderId, Optional<String> name, Optional<CourseCode> code) { }
