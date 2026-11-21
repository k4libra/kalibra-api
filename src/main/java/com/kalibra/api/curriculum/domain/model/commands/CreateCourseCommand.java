package com.kalibra.api.curriculum.domain.model.commands;

import com.kalibra.api.curriculum.domain.model.valueobjects.CourseCode;
import com.kalibra.api.curriculum.domain.model.valueobjects.SubtopicDefinition;

import java.util.List;

public record CreateCourseCommand(String holderId, String name, CourseCode code, List<String> subtopicNames,
        String term, String faculty, String semester,
        List<SubtopicDefinition> subtopics) {
    public CreateCourseCommand(String holderId, String name, CourseCode code, List<String> subtopicNames) {
        this(holderId, name, code, subtopicNames, null, null, null, null);
    }
}
