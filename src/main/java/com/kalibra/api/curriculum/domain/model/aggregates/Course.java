package com.kalibra.api.curriculum.domain.model.aggregates;

import com.kalibra.api.curriculum.domain.exceptions.CourseWithoutSubtopicsException;
import com.kalibra.api.curriculum.domain.model.commands.CreateCourseCommand;
import com.kalibra.api.curriculum.domain.model.entities.Subtopic;
import com.kalibra.api.curriculum.domain.model.valueobjects.CourseCode;
import com.kalibra.api.curriculum.domain.model.valueobjects.CourseId;
import com.kalibra.api.curriculum.domain.model.valueobjects.SubtopicId;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Course {

    private CourseId id;
    private String holderId;
    private String name;
    private CourseCode code;
    private List<Subtopic> subtopics;
    private Instant createdAt;

    // public: required by MapStruct, which generates its mapper impl in a different package.
    public Course() {
    }

    private Course(CreateCourseCommand command, List<Subtopic> subtopics) {
        this.id = new CourseId(UUID.randomUUID());
        this.holderId = command.holderId();
        this.name = command.name().trim();
        this.code = command.code();
        this.subtopics = subtopics;
        this.createdAt = Instant.now();
    }

    public static Course create(CreateCourseCommand command) {
        if (command.subtopicNames() == null || command.subtopicNames().isEmpty()) {
            throw new CourseWithoutSubtopicsException();
        }
        if (command.name() == null || command.name().isBlank()) {
            throw new IllegalArgumentException("Course name cannot be blank");
        }
        var subtopics = new ArrayList<Subtopic>();
        for (var position = 0; position < command.subtopicNames().size(); position++) {
            var subtopicId = new SubtopicId(UUID.randomUUID());
            subtopics.add(new Subtopic(subtopicId, command.subtopicNames().get(position), position + 1));
        }
        return new Course(command, subtopics);
    }

    public boolean hasSubtopic(SubtopicId subtopicId) {
        return subtopics.stream().anyMatch(subtopic -> subtopic.getId().equals(subtopicId));
    }

    public boolean isOwnedBy(String holderId) {
        return this.holderId.equals(holderId);
    }

    public CourseId getId() {
        return id;
    }

    public String getHolderId() {
        return holderId;
    }

    public String getName() {
        return name;
    }

    public CourseCode getCode() {
        return code;
    }

    public List<Subtopic> getSubtopics() {
        return subtopics;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setId(CourseId id) {
        this.id = id;
    }

    public void setHolderId(String holderId) {
        this.holderId = holderId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setCode(CourseCode code) {
        this.code = code;
    }

    public void setSubtopics(List<Subtopic> subtopics) {
        this.subtopics = subtopics;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
