package com.kalibra.api.curriculum.domain.model.commands;

import com.kalibra.api.curriculum.domain.model.valueobjects.CourseCode;

import java.util.List;

public record CreateCourseCommand(String holderId, String name, CourseCode code, List<String> subtopicNames) { }
