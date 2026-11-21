package com.kalibra.api.progress.domain.model.valueobjects;

import java.util.List;

public record InstitutionalIndicators(
        InstitutionalIndicatorTotals totals,
        List<CourseIndicatorSummary> courses
) {

    public InstitutionalIndicators {
        courses = List.copyOf(courses);
    }
}
