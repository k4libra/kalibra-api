package com.kalibra.api.curriculum.domain.model.entities;

import com.kalibra.api.curriculum.domain.model.valueobjects.SubtopicId;

public class Subtopic {

    private SubtopicId id;
    private String name;
    private int displayOrder;

    // public: required by MapStruct, which generates its mapper impl in a different package.
    public Subtopic() {
    }

    public Subtopic(SubtopicId id, String name, int displayOrder) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Subtopic name cannot be blank");
        }
        this.id = id;
        this.name = name.trim();
        this.displayOrder = displayOrder;
    }

    public SubtopicId getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public void setId(SubtopicId id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }
}
