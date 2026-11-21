package com.kalibra.api.progress.interfaces.rest.transform;

import com.kalibra.api.progress.domain.model.valueobjects.AccuracyIndicator;
import com.kalibra.api.progress.domain.model.valueobjects.CourseIndicatorsReport;
import com.kalibra.api.progress.domain.model.valueobjects.Feedback;
import com.kalibra.api.progress.domain.model.valueobjects.IndicatorExportRow;
import com.kalibra.api.progress.domain.model.valueobjects.IndicatorGuide;
import com.kalibra.api.progress.domain.model.valueobjects.IndicatorGuideEntry;
import com.kalibra.api.progress.domain.model.valueobjects.IndicatorsCsvExport;
import com.kalibra.api.progress.domain.model.valueobjects.MasteryEvolutionIndicator;
import com.kalibra.api.progress.domain.model.valueobjects.MasteryGapMap;
import com.kalibra.api.progress.domain.model.valueobjects.PracticeIndicator;
import com.kalibra.api.progress.domain.model.valueobjects.PracticeSubtopicView;
import com.kalibra.api.progress.domain.model.valueobjects.StudentAccuracyLine;
import com.kalibra.api.progress.domain.model.valueobjects.StudentMasteryCell;
import com.kalibra.api.progress.domain.model.valueobjects.StudentProgressReport;
import com.kalibra.api.progress.domain.model.valueobjects.SubtopicApprovalLine;
import com.kalibra.api.progress.domain.model.valueobjects.SubtopicEvolutionLine;
import com.kalibra.api.progress.domain.model.valueobjects.SubtopicGapLine;
import com.kalibra.api.progress.domain.model.valueobjects.SubtopicPracticeLine;
import com.kalibra.api.progress.domain.model.valueobjects.SubtopicProgressLine;
import com.kalibra.api.progress.domain.model.valueobjects.VerificationIndicator;
import com.kalibra.api.progress.interfaces.rest.resources.AccuracyIndicatorResource;
import com.kalibra.api.progress.interfaces.rest.resources.CourseIndicatorReportResource;
import com.kalibra.api.progress.interfaces.rest.resources.IndicatorExportResource;
import com.kalibra.api.progress.interfaces.rest.resources.IndicatorGuideEntryResource;
import com.kalibra.api.progress.interfaces.rest.resources.IndicatorGuideResource;
import com.kalibra.api.progress.interfaces.rest.resources.MasteryEvolutionResource;
import com.kalibra.api.progress.interfaces.rest.resources.MasteryGapMapResource;
import com.kalibra.api.progress.interfaces.rest.resources.PracticeIndicatorResource;
import com.kalibra.api.progress.interfaces.rest.resources.StudentAccuracyResource;
import com.kalibra.api.progress.interfaces.rest.resources.StudentMasteryResource;
import com.kalibra.api.progress.interfaces.rest.resources.StudentProgressResource;
import com.kalibra.api.progress.interfaces.rest.resources.SubtopicApprovalResource;
import com.kalibra.api.progress.interfaces.rest.resources.SubtopicEvolutionResource;
import com.kalibra.api.progress.interfaces.rest.resources.SubtopicGapResource;
import com.kalibra.api.progress.interfaces.rest.resources.SubtopicMasteryResource;
import com.kalibra.api.progress.interfaces.rest.resources.SubtopicPracticeResource;
import com.kalibra.api.progress.interfaces.rest.resources.SubtopicProgressResource;
import com.kalibra.api.progress.interfaces.rest.resources.VerificationIndicatorResource;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Locale;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface SubtopicMasteryAssembler {

    String CSV_HEADER = "student_code,subtopic,exercises_solved,accuracy_percentage,"
            + "initial_mastery_percentage,current_mastery_percentage,verification_approval_percentage";

    SubtopicMasteryResource toResource(PracticeSubtopicView view);

    StudentProgressResource toResource(StudentProgressReport report);

    @Mapping(target = "mastery", expression = "java(line.mastery().map(value -> (double) value.asPercentage()).orElse(null))")
    SubtopicProgressResource toResource(SubtopicProgressLine line);

    MasteryGapMapResource toResource(MasteryGapMap map);

    SubtopicGapResource toResource(SubtopicGapLine line);

    @Mapping(target = "mastery", expression = "java(cell.mastery().map(value -> (double) value.asPercentage()).orElse(null))")
    StudentMasteryResource toResource(StudentMasteryCell cell);

    CourseIndicatorReportResource toResource(CourseIndicatorsReport report);

    AccuracyIndicatorResource toResource(AccuracyIndicator indicator);

    @Mapping(target = "accuracy", expression = "java(line.accuracy().orElse(null))")
    StudentAccuracyResource toResource(StudentAccuracyLine line);

    PracticeIndicatorResource toResource(PracticeIndicator indicator);

    SubtopicPracticeResource toResource(SubtopicPracticeLine line);

    MasteryEvolutionResource toResource(MasteryEvolutionIndicator indicator);

    @Mapping(target = "initialAverage", expression = "java(line.initialAverage().orElse(null))")
    @Mapping(target = "currentAverage", expression = "java(line.currentAverage().orElse(null))")
    @Mapping(target = "deltaPoints", expression = "java(line.deltaPoints().orElse(null))")
    SubtopicEvolutionResource toResource(SubtopicEvolutionLine line);

    VerificationIndicatorResource toResource(VerificationIndicator indicator);

    SubtopicApprovalResource toResource(SubtopicApprovalLine line);

    IndicatorGuideResource toResource(IndicatorGuide guide);

    IndicatorGuideEntryResource toResource(IndicatorGuideEntry entry);

    default IndicatorExportResource toResource(IndicatorsCsvExport export) {
        var rows = export.rows().stream()
                .map(this::toCsvRow)
                .collect(Collectors.joining("\n"));
        return new IndicatorExportResource(export.fileName(), CSV_HEADER + "\n" + rows + (rows.isEmpty() ? "" : "\n"));
    }

    default String toCsvRow(IndicatorExportRow row) {
        return String.join(",",
                row.studentCode().value(),
                escape(row.subtopicName()),
                String.valueOf(row.solved()),
                decimal(row.accuracy()),
                decimal(row.initialMastery()),
                decimal(row.currentMastery()),
                decimal(row.verificationApprovalRate()));
    }

    // required by MapStruct: single-field VOs need an explicit converter.
    default String map(Feedback feedback) {
        return feedback == null ? null : feedback.explanation();
    }

    private static String decimal(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    // Quotes the text and neutralizes a leading formula character, so a subtopic name can
    // never run as a formula when the file is opened in a spreadsheet.
    private static String escape(String text) {
        var value = text == null ? "" : text;
        if (!value.isEmpty() && "=+-@".indexOf(value.charAt(0)) >= 0) {
            value = "'" + value;
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
