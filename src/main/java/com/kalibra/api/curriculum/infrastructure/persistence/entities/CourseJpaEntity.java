package com.kalibra.api.curriculum.infrastructure.persistence.entities;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "courses", schema = "curriculum")
public class CourseJpaEntity {

    @Id
    private UUID id;

    @Column(name = "holder_id", nullable = false, length = 64)
    private String holderId;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 20)
    private String code;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "course_subtopics", schema = "curriculum", joinColumns = @JoinColumn(name = "course_id"))
    @OrderBy("displayOrder ASC")
    private List<SubtopicEmbeddable> subtopics = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    // public: required by MapStruct, which generates its mapper impl in a different package.
    public CourseJpaEntity() {
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public List<SubtopicEmbeddable> getSubtopics() {
        return subtopics;
    }

    public void setSubtopics(List<SubtopicEmbeddable> subtopics) {
        this.subtopics = subtopics;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
