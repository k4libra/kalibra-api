package com.kalibra.api.progress.domain.model.valueobjects;

import java.util.List;

public record AccuracyIndicator(
        double groupAccuracy,
        List<StudentAccuracyLine> perStudent
) {

    public AccuracyIndicator {
        perStudent = List.copyOf(perStudent);
    }
}
