package com.kalibra.api.curriculum.infrastructure.persistence.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "teacher_workspaces", schema = "curriculum")
public class TeacherWorkspaceJpaEntity {

    @Id
    private UUID id;

    @Column(name = "holder_id", nullable = false, unique = true, length = 64)
    private String holderId;

    @Column(name = "active_course_id")
    private UUID activeCourseId;

    // public: required by MapStruct, which generates its mapper impl in a different package.
    public TeacherWorkspaceJpaEntity() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getHolderId() {
        return holderId;
    }

    public void setHolderId(String holderId) {
        this.holderId = holderId;
    }

    public UUID getActiveCourseId() {
        return activeCourseId;
    }

    public void setActiveCourseId(UUID activeCourseId) {
        this.activeCourseId = activeCourseId;
    }
}
