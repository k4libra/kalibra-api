package com.kalibra.api.curriculum.infrastructure.persistence.entities;

import com.kalibra.api.curriculum.domain.model.valueobjects.SubtopicStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.util.UUID;

@Embeddable
public class SubtopicEmbeddable {

    @Column(name = "subtopic_id", nullable = false)
    private UUID subtopicId;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SubtopicStatus status = SubtopicStatus.ACTIVE;

    // public: required by MapStruct, which generates its mapper impl in a different package.
    public SubtopicEmbeddable() {
    }

    public SubtopicStatus getStatus() {
        return status;
    }

    public void setStatus(SubtopicStatus status) {
        this.status = status;
    }

    public UUID getSubtopicId() {
        return subtopicId;
    }

    public void setSubtopicId(UUID subtopicId) {
        this.subtopicId = subtopicId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }
}
