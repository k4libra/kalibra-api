package com.kalibra.api.curriculum.domain.model.aggregates;

import com.kalibra.api.curriculum.domain.model.commands.SelectActiveCourseCommand;
import com.kalibra.api.curriculum.domain.model.valueobjects.CourseId;

import java.util.Optional;
import java.util.UUID;

public class TeacherWorkspace {

    private UUID id;
    private String holderId;
    private Optional<CourseId> activeCourseId;

    // public: required by MapStruct, which generates its mapper impl in a different package.
    public TeacherWorkspace() {
    }

    private TeacherWorkspace(String holderId) {
        this.id = UUID.randomUUID();
        this.holderId = holderId;
        this.activeCourseId = Optional.empty();
    }

    public static TeacherWorkspace createFor(String holderId) {
        return new TeacherWorkspace(holderId);
    }

    public boolean activate(SelectActiveCourseCommand command) {
        if (activeCourseId.filter(command.courseId()::equals).isPresent()) {
            return false;
        }
        this.activeCourseId = Optional.of(command.courseId());
        return true;
    }

    public UUID getId() {
        return id;
    }

    public String getHolderId() {
        return holderId;
    }

    public Optional<CourseId> getActiveCourseId() {
        return activeCourseId;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public void setHolderId(String holderId) {
        this.holderId = holderId;
    }

    public void setActiveCourseId(Optional<CourseId> activeCourseId) {
        this.activeCourseId = activeCourseId;
    }
}
