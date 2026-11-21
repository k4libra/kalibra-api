package com.kalibra.api.curriculum.infrastructure.persistence.entities;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "curricular_materials", schema = "curriculum")
public class CurricularMaterialJpaEntity {

    @Id
    private UUID id;

    @Column(name = "course_id", nullable = false)
    private UUID courseId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "curricular_material_subtopics", schema = "curriculum",
            joinColumns = @JoinColumn(name = "material_id"))
    @Column(name = "subtopic_id", nullable = false)
    private Set<UUID> subtopicIds = new HashSet<>();

    @Embedded
    private MaterialFileEmbeddable file;

    @Column(nullable = false, length = 30)
    private String status;

    @Embedded
    private CurricularContentEmbeddable content;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "uploaded_at", nullable = false)
    private Instant uploadedAt;

    // public: required by MapStruct, which generates its mapper impl in a different package.
    public CurricularMaterialJpaEntity() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getCourseId() {
        return courseId;
    }

    public void setCourseId(UUID courseId) {
        this.courseId = courseId;
    }

    public Set<UUID> getSubtopicIds() {
        return subtopicIds;
    }

    public void setSubtopicIds(Set<UUID> subtopicIds) {
        this.subtopicIds = subtopicIds;
    }

    public MaterialFileEmbeddable getFile() {
        return file;
    }

    public void setFile(MaterialFileEmbeddable file) {
        this.file = file;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public CurricularContentEmbeddable getContent() {
        return content;
    }

    public void setContent(CurricularContentEmbeddable content) {
        this.content = content;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public Instant getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(Instant uploadedAt) {
        this.uploadedAt = uploadedAt;
    }
}
