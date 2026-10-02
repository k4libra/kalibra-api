package com.kalibra.api.progress.domain.services;

import com.kalibra.api.progress.domain.model.aggregates.SubtopicMastery;
import com.kalibra.api.progress.domain.model.valueobjects.MasteryGapMap;
import com.kalibra.api.progress.domain.model.valueobjects.MasteryLevel;
import com.kalibra.api.progress.domain.model.valueobjects.StudentId;
import com.kalibra.api.progress.domain.model.valueobjects.StudentMasteryCell;
import com.kalibra.api.progress.domain.model.valueobjects.SubtopicGapLine;
import com.kalibra.api.progress.domain.model.valueobjects.SubtopicId;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class MasteryGapAnalyzer {

    // Subtopic names and student emails belong to other contexts: lines and cells come out
    // without them and the caller fills them in.
    public MasteryGapMap analyze(List<SubtopicMastery> masteries, List<StudentId> roster) {
        Set<String> enrolled = roster.stream()
                .map(student -> student.value().toString())
                .collect(Collectors.toSet());
        var ofRoster = masteries.stream()
                .filter(mastery -> enrolled.contains(mastery.getHolderId()))
                .toList();
        var subtopicIds = ofRoster.stream()
                .map(SubtopicMastery::getSubtopicId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        var lines = new ArrayList<SubtopicGapLine>();
        var cells = new ArrayList<StudentMasteryCell>();
        for (var subtopicId : subtopicIds) {
            var ofSubtopic = ofRoster.stream()
                    .filter(mastery -> mastery.getSubtopicId().equals(subtopicId))
                    .toList();
            lines.add(toLine(subtopicId, ofSubtopic, roster.size()));
            for (var student : roster) {
                cells.add(toCell(student, subtopicId, ofSubtopic));
            }
        }
        var courseId = masteries.isEmpty() ? null : masteries.getFirst().getCourseId().value();
        return new MasteryGapMap(courseId, !ofRoster.isEmpty(), prioritize(lines), cells);
    }

    private SubtopicGapLine toLine(SubtopicId subtopicId, List<SubtopicMastery> ofSubtopic, int rosterSize) {
        var groupMastery = ofSubtopic.stream()
                .mapToDouble(mastery -> mastery.getCurrentEstimate().value() * 100)
                .average()
                .orElse(0);
        return new SubtopicGapLine(
                subtopicId.value(),
                null,
                Math.round(groupMastery * 10) / 10.0,
                count(ofSubtopic, MasteryLevel.LOW),
                count(ofSubtopic, MasteryLevel.MEDIUM),
                count(ofSubtopic, MasteryLevel.HIGH),
                rosterSize - ofSubtopic.size(),
                0
        );
    }

    private StudentMasteryCell toCell(StudentId student, SubtopicId subtopicId, List<SubtopicMastery> ofSubtopic) {
        var mastery = ofSubtopic.stream()
                .filter(candidate -> candidate.getHolderId().equals(student.value().toString()))
                .findFirst();
        return new StudentMasteryCell(
                student.value(),
                null,
                subtopicId.value(),
                mastery.map(SubtopicMastery::getCurrentEstimate),
                mastery.map(SubtopicMastery::getLevel).orElse(MasteryLevel.NO_DATA)
        );
    }

    // Priority 1 is the subtopic to reinforce first: the lowest group mastery and, on a tie,
    // the one with more students at a low level.
    private List<SubtopicGapLine> prioritize(List<SubtopicGapLine> lines) {
        var ordered = lines.stream()
                .sorted(Comparator.comparingDouble(SubtopicGapLine::groupMastery)
                        .thenComparing(Comparator.comparingInt(SubtopicGapLine::lowCount).reversed()))
                .toList();
        var prioritized = new ArrayList<SubtopicGapLine>();
        for (var position = 0; position < ordered.size(); position++) {
            var line = ordered.get(position);
            prioritized.add(new SubtopicGapLine(line.subtopicId(), line.subtopicName(), line.groupMastery(),
                    line.lowCount(), line.mediumCount(), line.highCount(), line.noDataCount(), position + 1));
        }
        return prioritized;
    }

    private int count(List<SubtopicMastery> masteries, MasteryLevel level) {
        return (int) masteries.stream()
                .filter(mastery -> mastery.getLevel() == level)
                .count();
    }
}
