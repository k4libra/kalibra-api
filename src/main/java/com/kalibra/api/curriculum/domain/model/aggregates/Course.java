package com.kalibra.api.curriculum.domain.model.aggregates;

import com.kalibra.api.curriculum.domain.exceptions.CourseWithoutSubtopicsException;
import com.kalibra.api.curriculum.domain.exceptions.LastActiveSubtopicException;
import com.kalibra.api.curriculum.domain.exceptions.SubtopicLockedForRemovalException;
import com.kalibra.api.curriculum.domain.exceptions.SubtopicLockedForRenameException;
import com.kalibra.api.curriculum.domain.exceptions.SubtopicNotFoundException;
import com.kalibra.api.curriculum.domain.model.commands.AddSubtopicCommand;
import com.kalibra.api.curriculum.domain.model.commands.CreateCourseCommand;
import com.kalibra.api.curriculum.domain.model.commands.RemoveSubtopicCommand;
import com.kalibra.api.curriculum.domain.model.commands.UpdateCourseCommand;
import com.kalibra.api.curriculum.domain.model.commands.UpdateSubtopicCommand;
import com.kalibra.api.curriculum.domain.model.entities.Subtopic;
import com.kalibra.api.curriculum.domain.model.valueobjects.CourseCode;
import com.kalibra.api.curriculum.domain.model.valueobjects.CourseId;
import com.kalibra.api.curriculum.domain.model.valueobjects.SubtopicId;
import com.kalibra.api.curriculum.domain.model.valueobjects.SubtopicStatus;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
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

    public void update(UpdateCourseCommand command) {
        command.name().ifPresent(Course::validateName);
        command.name().ifPresent(value -> name = value.trim());
        command.code().ifPresent(value -> code = value);
    }

    public void addSubtopic(AddSubtopicCommand command) {
        validateName(command.name());
        var updated = new ArrayList<>(subtopics);
        updated.add(new Subtopic(new SubtopicId(UUID.randomUUID()), command.name(), updated.size() + 1));
        subtopics = updated;
    }

    public void updateSubtopic(UpdateSubtopicCommand command, boolean locked) {
        var subtopic = subtopic(command.subtopicId());
        if (command.name().isPresent() && locked) {
            throw new SubtopicLockedForRenameException();
        }
        command.name().ifPresent(Course::validateName);
        command.displayOrder().ifPresent(order -> {
            if (order < 1 || order > subtopics.size()) {
                throw new IllegalArgumentException("Display order must be between 1 and the number of subtopics");
            }
        });
        if (command.status().filter(status -> status == SubtopicStatus.ARCHIVED).isPresent()) {
            requireAnotherActiveSubtopic(subtopic);
        }
        command.name().ifPresent(value -> subtopic.setName(value.trim()));
        command.status().ifPresent(subtopic::setStatus);
        command.displayOrder().ifPresent(order -> {
            var updated = new ArrayList<>(subtopics);
            updated.sort(Comparator.comparingInt(Subtopic::getDisplayOrder));
            updated.remove(subtopic);
            updated.add(order - 1, subtopic);
            subtopics = updated;
            normalizeOrder();
        });
    }

    public void removeSubtopic(RemoveSubtopicCommand command, boolean locked) {
        var subtopic = subtopic(command.subtopicId());
        if (locked) {
            throw new SubtopicLockedForRemovalException();
        }
        requireAnotherActiveSubtopic(subtopic);
        var updated = new ArrayList<>(subtopics);
        updated.remove(subtopic);
        subtopics = updated;
        normalizeOrder();
    }

    public boolean hasActiveSubtopic(SubtopicId subtopicId) {
        return subtopics.stream().anyMatch(subtopic -> subtopic.getId().equals(subtopicId) && subtopic.isActive());
    }

    private Subtopic subtopic(SubtopicId subtopicId) {
        return subtopics.stream().filter(found -> found.getId().equals(subtopicId)).findFirst()
                .orElseThrow(() -> new SubtopicNotFoundException(subtopicId.value()));
    }

    private void requireAnotherActiveSubtopic(Subtopic subtopic) {
        if (subtopic.isActive() && subtopics.stream().filter(Subtopic::isActive).count() == 1) {
            throw new LastActiveSubtopicException();
        }
    }

    private void normalizeOrder() {
        for (int index = 0; index < subtopics.size(); index++) {
            subtopics.get(index).setDisplayOrder(index + 1);
        }
    }

    private static void validateName(String name) {
        if (name == null || name.isBlank() || name.length() > 120) {
            throw new IllegalArgumentException("Name must contain between 1 and 120 characters");
        }
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
