package com.kalibra.api.progress.domain.services;

import com.kalibra.api.progress.domain.model.aggregates.ExerciseAttempt;
import com.kalibra.api.progress.domain.model.aggregates.SubtopicMastery;
import com.kalibra.api.progress.domain.model.valueobjects.AccuracyIndicator;
import com.kalibra.api.progress.domain.model.valueobjects.CourseIndicatorsReport;
import com.kalibra.api.progress.domain.model.valueobjects.MasteryEvolutionIndicator;
import com.kalibra.api.progress.domain.model.valueobjects.MasteryLevel;
import com.kalibra.api.progress.domain.model.valueobjects.PracticeIndicator;
import com.kalibra.api.progress.domain.model.valueobjects.StudentAccuracyLine;
import com.kalibra.api.progress.domain.model.valueobjects.SubtopicEvolutionLine;
import com.kalibra.api.progress.domain.model.valueobjects.SubtopicId;
import com.kalibra.api.progress.domain.model.valueobjects.SubtopicPracticeLine;
import com.kalibra.api.progress.domain.model.valueobjects.VerificationIndicator;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;

public class CourseIndicatorsCalculator {

    // Works only with what progress owns. Students without activity, subtopics without
    // practice, names, emails and the verification indicator come from other contexts and
    // are completed by the caller.
    public CourseIndicatorsReport calculate(List<ExerciseAttempt> attempts, List<SubtopicMastery> masteries) {
        var courseId = attempts.stream().findFirst().map(attempt -> attempt.getCourseId().value())
                .or(() -> masteries.stream().findFirst().map(mastery -> mastery.getCourseId().value()))
                .orElse(null);
        return new CourseIndicatorsReport(
                courseId,
                !attempts.isEmpty(),
                accuracyOf(attempts),
                practiceOf(attempts),
                evolutionOf(masteries),
                new VerificationIndicator(0, 0, 0, List.of())
        );
    }

    public static double percentage(long part, long total) {
        return total == 0 ? 0 : round(part * 100.0 / total);
    }

    public static double round(double value) {
        return Math.round(value * 10) / 10.0;
    }

    private AccuracyIndicator accuracyOf(List<ExerciseAttempt> attempts) {
        Map<String, List<ExerciseAttempt>> byStudent = attempts.stream()
                .collect(Collectors.groupingBy(ExerciseAttempt::getHolderId, LinkedHashMap::new, Collectors.toList()));
        var lines = byStudent.entrySet().stream()
                .map(entry -> {
                    var correct = correctOf(entry.getValue());
                    var submitted = entry.getValue().size();
                    return new StudentAccuracyLine(UUID.fromString(entry.getKey()), null, correct, submitted,
                            Optional.of(percentage(correct, submitted)), true);
                })
                .toList();
        return new AccuracyIndicator(percentage(correctOf(attempts), attempts.size()), lines);
    }

    private PracticeIndicator practiceOf(List<ExerciseAttempt> attempts) {
        var activeStudents = (int) attempts.stream().map(ExerciseAttempt::getHolderId).distinct().count();
        Map<SubtopicId, Long> bySubtopic = attempts.stream()
                .collect(Collectors.groupingBy(ExerciseAttempt::getSubtopicId, LinkedHashMap::new, Collectors.counting()));
        var lines = bySubtopic.entrySet().stream()
                .map(entry -> new SubtopicPracticeLine(entry.getKey().value(), null, entry.getValue().intValue()))
                .toList();
        var average = activeStudents == 0 ? 0 : round((double) attempts.size() / activeStudents);
        return new PracticeIndicator(attempts.size(), average, activeStudents, activeStudents, lines);
    }

    private MasteryEvolutionIndicator evolutionOf(List<SubtopicMastery> masteries) {
        Map<SubtopicId, List<SubtopicMastery>> bySubtopic = masteries.stream()
                .collect(Collectors.groupingBy(SubtopicMastery::getSubtopicId, LinkedHashMap::new, Collectors.toList()));
        var lines = bySubtopic.entrySet().stream()
                .map(entry -> {
                    var initial = average(entry.getValue(), mastery -> mastery.getInitialEstimate().value());
                    var current = average(entry.getValue(), mastery -> mastery.getCurrentEstimate().value());
                    return new SubtopicEvolutionLine(entry.getKey().value(), null, Optional.of(initial), Optional.of(current),
                            Optional.of(delta(initial, current)), MasteryLevel.ofPercentage(current), true);
                })
                .toList();
        var groupInitial = average(masteries, mastery -> mastery.getInitialEstimate().value());
        var groupCurrent = average(masteries, mastery -> mastery.getCurrentEstimate().value());
        return new MasteryEvolutionIndicator(groupInitial, groupCurrent, delta(groupInitial, groupCurrent), lines);
    }

    private int correctOf(List<ExerciseAttempt> attempts) {
        return (int) attempts.stream().filter(ExerciseAttempt::isCorrect).count();
    }

    private double average(List<SubtopicMastery> masteries, ToDoubleFunction<SubtopicMastery> probability) {
        return round(masteries.stream().mapToDouble(probability).average().orElse(0) * 100);
    }

    private int delta(double initial, double current) {
        return (int) Math.round(current - initial);
    }
}
