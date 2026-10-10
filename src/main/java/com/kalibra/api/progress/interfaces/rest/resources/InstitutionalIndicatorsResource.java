package com.kalibra.api.progress.interfaces.rest.resources;

import java.util.List;

public record InstitutionalIndicatorsResource(
        InstitutionalIndicatorTotalsResource totals,
        List<CourseIndicatorSummaryResource> courses
) {
}
