package com.kalibra.api.progress.interfaces.rest.transform;

import com.kalibra.api.progress.domain.model.valueobjects.CourseIndicatorSummary;
import com.kalibra.api.progress.domain.model.valueobjects.CriticalSubtopicLine;
import com.kalibra.api.progress.domain.model.valueobjects.InstitutionalIndicatorTotals;
import com.kalibra.api.progress.domain.model.valueobjects.InstitutionalIndicators;
import com.kalibra.api.progress.interfaces.rest.resources.CourseIndicatorSummaryResource;
import com.kalibra.api.progress.interfaces.rest.resources.CriticalSubtopicResource;
import com.kalibra.api.progress.interfaces.rest.resources.InstitutionalIndicatorTotalsResource;
import com.kalibra.api.progress.interfaces.rest.resources.InstitutionalIndicatorsResource;
import org.mapstruct.Mapper;

import java.util.Locale;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface InstitutionalIndicatorsAssembler {

    // Course-level aggregates only: the export never carries teacher or student identities.
    String CSV_HEADER = "course_code,course_name,enrolled_students,active_students,exercises_solved,"
            + "accuracy_percentage,mastery_delta_points,verification_approval_percentage";

    InstitutionalIndicatorsResource toResource(InstitutionalIndicators indicators);

    InstitutionalIndicatorTotalsResource toResource(InstitutionalIndicatorTotals totals);

    CourseIndicatorSummaryResource toResource(CourseIndicatorSummary summary);

    CriticalSubtopicResource toResource(CriticalSubtopicLine line);

    default String toCsv(InstitutionalIndicators indicators) {
        var rows = indicators.courses().stream()
                .map(course -> String.join(",",
                        escape(course.code()),
                        escape(course.name()),
                        String.valueOf(course.enrolledStudents()),
                        String.valueOf(course.activeStudents()),
                        String.valueOf(course.totalSolved()),
                        decimal(course.groupAccuracy()),
                        String.valueOf(course.groupDeltaPoints()),
                        decimal(course.verificationApprovalRate())))
                .collect(Collectors.joining("\n"));
        return CSV_HEADER + "\n" + rows + (rows.isEmpty() ? "" : "\n");
    }

    private static String decimal(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    // Quotes the text and neutralizes a leading formula character (CSV injection).
    private static String escape(String text) {
        var value = text == null ? "" : text;
        if (!value.isEmpty() && "=+-@".indexOf(value.charAt(0)) >= 0) {
            value = "'" + value;
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
