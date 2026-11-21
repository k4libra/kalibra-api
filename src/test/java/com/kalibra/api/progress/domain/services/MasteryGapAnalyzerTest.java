package com.kalibra.api.progress.domain.services;

import com.kalibra.api.progress.ProgressFixtures;
import com.kalibra.api.progress.domain.model.valueobjects.CourseId;
import com.kalibra.api.progress.domain.model.valueobjects.MasteryLevel;
import com.kalibra.api.progress.domain.model.valueobjects.StudentId;
import com.kalibra.api.progress.domain.model.valueobjects.SubtopicGapLine;
import com.kalibra.api.progress.domain.model.valueobjects.SubtopicId;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MasteryGapAnalyzerTest {

    private final MasteryGapAnalyzer analyzer = new MasteryGapAnalyzer();

    private final CourseId courseId = new CourseId(UUID.randomUUID());
    private final SubtopicId equations = new SubtopicId(UUID.randomUUID());
    private final SubtopicId inequalities = new SubtopicId(UUID.randomUUID());
    private final StudentId ana = new StudentId(UUID.randomUUID());
    private final StudentId luis = new StudentId(UUID.randomUUID());
    private final StudentId rosa = new StudentId(UUID.randomUUID());

    private String holder(StudentId student) {
        return student.value().toString();
    }

    @Test
    void shouldAggregateTheGroupAndPrioritizeTheWeakestSubtopic() {
        // Arrange
        var masteries = List.of(
                ProgressFixtures.mastery(holder(ana), courseId, equations, 0.30, 0.80, MasteryLevel.HIGH),
                ProgressFixtures.mastery(holder(luis), courseId, equations, 0.30, 0.60, MasteryLevel.MEDIUM),
                ProgressFixtures.mastery(holder(ana), courseId, inequalities, 0.30, 0.20, MasteryLevel.LOW),
                ProgressFixtures.mastery(holder(luis), courseId, inequalities, 0.30, 0.30, MasteryLevel.LOW));

        // Act
        var map = analyzer.analyze(masteries, List.of(ana, luis, rosa));

        // Assert
        assertThat(map.hasSufficientData()).isTrue();
        assertThat(map.courseId()).isEqualTo(courseId.value());
        assertThat(map.subtopics()).extracting(SubtopicGapLine::subtopicId)
                .containsExactly(inequalities.value(), equations.value());
        var weakest = map.subtopics().getFirst();
        assertThat(weakest.reinforcementPriority()).isEqualTo(1);
        assertThat(weakest.groupMastery()).isEqualTo(25.0);
        assertThat(weakest.lowCount()).isEqualTo(2);
        assertThat(weakest.noDataCount()).isEqualTo(1);
        var strongest = map.subtopics().get(1);
        assertThat(strongest.reinforcementPriority()).isEqualTo(2);
        assertThat(strongest.groupMastery()).isEqualTo(70.0);
        assertThat(strongest.mediumCount()).isEqualTo(1);
        assertThat(strongest.highCount()).isEqualTo(1);
        assertThat(strongest.noDataCount()).isEqualTo(1);
    }

    @Test
    void shouldGiveACellPerStudentAndSubtopicWithNoDataForThoseWhoHaveNotPracticed() {
        var masteries = List.of(ProgressFixtures.mastery(holder(ana), courseId, equations, 0.30, 0.80, MasteryLevel.HIGH));

        var map = analyzer.analyze(masteries, List.of(ana, rosa));

        assertThat(map.students()).hasSize(2);
        assertThat(map.students().getFirst().studentId()).isEqualTo(ana.value());
        assertThat(map.students().getFirst().level()).isEqualTo(MasteryLevel.HIGH);
        assertThat(map.students().getFirst().mastery()).isPresent();
        assertThat(map.students().get(1).studentId()).isEqualTo(rosa.value());
        assertThat(map.students().get(1).level()).isEqualTo(MasteryLevel.NO_DATA);
        assertThat(map.students().get(1).mastery()).isEmpty();
    }

    @Test
    void shouldReportInsufficientDataWithoutEstimates() {
        var map = analyzer.analyze(List.of(), List.of(ana, luis));

        assertThat(map.hasSufficientData()).isFalse();
        assertThat(map.subtopics()).isEmpty();
        assertThat(map.students()).isEmpty();
    }

    @Test
    void shouldIgnoreTheEstimatesOfStudentsOutsideTheRoster() {
        var masteries = List.of(ProgressFixtures.mastery(holder(rosa), courseId, equations, 0.30, 0.80, MasteryLevel.HIGH));

        var map = analyzer.analyze(masteries, List.of(ana));

        assertThat(map.hasSufficientData()).isFalse();
        assertThat(map.subtopics()).isEmpty();
    }
}
