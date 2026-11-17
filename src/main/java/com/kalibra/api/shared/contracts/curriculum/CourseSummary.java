package com.kalibra.api.shared.contracts.curriculum;

import java.util.UUID;

public record CourseSummary(UUID courseId, String name, String code, String holderId, String term, String faculty, String semester) {
    public CourseSummary(UUID courseId, String name, String code, String holderId) {
        this(courseId, name, code, holderId, null, null, null);
    }
}
