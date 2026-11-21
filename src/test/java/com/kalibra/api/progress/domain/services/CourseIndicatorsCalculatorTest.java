package com.kalibra.api.progress.domain.services;

import com.kalibra.api.progress.ProgressFixtures;
import com.kalibra.api.progress.domain.model.valueobjects.CourseId;
import com.kalibra.api.progress.domain.model.valueobjects.MasteryLevel;
import com.kalibra.api.progress.domain.model.valueobjects.SubtopicId;
import com.kalibra.api.progress.domain.model.valueobjects.SubtopicPracticeLine;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CourseIndicatorsCalculatorTest {

    private final CourseIndicatorsCalculator calculator = new CourseIndicatorsCalculator();

    private final CourseId courseId = new CourseId(UUID.randomUUID());
    private final SubtopicId equations = new SubtopicId(UUID.randomUUID());
    private final SubtopicId inequalities = new SubtopicId(UUID.randomUUID());
    private final String ana = UUID.randomUUID().toString();
    private final String luis = UUID.randomUUID().toString();

    @Test
    void shouldComputeTheAccuracyAsCorrectOverSubmittedPerStudentAndForTheGroup() {
        var attempts = List.of(
                ProgressFixtures.attempt(ana, courseId, equations, "B", 0.30, 0.50),
                ProgressFixtures.attempt(ana, courseId, equations, "B", 0.50, 0.65),
                ProgressFixtures.attempt(ana, courseId, equations, "A", 0.65, 0.55),
                ProgressFixtures.attempt(luis, courseId, inequalities, "A", 0.30, 0.22));

        var accuracy = calculator.calculate(attempts, List.of()).accuracy();

        assertThat(accuracy.groupAccuracy()).isEqualTo(50.0);
        assertThat(accuracy.perStudent()).hasSize(2);
        var anaLine = accuracy.perStudent().getFirst();
        assertThat(anaLine.studentId()).isEqualTo(UUID.fromString(ana));
        assertThat(anaLine.correct()).isEqualTo(2);
        assertThat(anaLine.submitted()).isEqualTo(3);
        assertThat(anaLine.accuracy()).contains(66.7);
        assertThat(anaLine.hasActivity()).isTrue();
        assertThat(accuracy.perStudent().get(1).accuracy()).contains(0.0);
    }

    @Test
    void shouldCountThePracticeInTotalPerSubtopicAndPerActiveStudent() {
        var attempts = List.of(
                ProgressFixtures.attempt(ana, courseId, equations, "B", 0.30, 0.50),
                ProgressFixtures.attempt(ana, courseId, equations, "B", 0.50, 0.65),
                ProgressFixtures.attempt(ana, courseId, inequalities, "A", 0.30, 0.22),
                ProgressFixtures.attempt(luis, courseId, inequalities, "A", 0.30, 0.22),
                ProgressFixtures.attempt(luis, courseId, inequalities, "B", 0.22, 0.45));

        var report = calculator.calculate(attempts, List.of());
        var practice = report.practice();

        assertThat(report.hasActivity()).isTrue();
        assertThat(report.courseId()).isEqualTo(courseId.value());
        assertThat(practice.totalSolved()).isEqualTo(5);
        assertThat(practice.activeStudents()).isEqualTo(2);
        assertThat(practice.averagePerActiveStudent()).isEqualTo(2.5);
        assertThat(practice.perSubtopic()).extracting(SubtopicPracticeLine::solved).containsExactly(2, 3);
        assertThat(practice.perSubtopic().stream().mapToInt(SubtopicPracticeLine::solved).sum())
                .isEqualTo(practice.totalSolved());
    }

    @Test
    void shouldCompareTheFirstEstimateWithTheCurrentOnePerSubtopicAndForTheGroup() {
        var masteries = List.of(
                ProgressFixtures.mastery(ana, courseId, equations, 0.30, 0.80, MasteryLevel.HIGH),
                ProgressFixtures.mastery(luis, courseId, equations, 0.40, 0.70, MasteryLevel.MEDIUM),
                ProgressFixtures.mastery(ana, courseId, inequalities, 0.50, 0.30, MasteryLevel.LOW));

        var evolution = calculator.calculate(List.of(), masteries).evolution();

        var equationsLine = evolution.perSubtopic().getFirst();
        assertThat(equationsLine.subtopicId()).isEqualTo(equations.value());
        assertThat(equationsLine.initialAverage()).contains(35.0);
        assertThat(equationsLine.currentAverage()).contains(75.0);
        assertThat(equationsLine.deltaPoints()).contains(40);
        assertThat(equationsLine.level()).isEqualTo(MasteryLevel.HIGH);
        assertThat(equationsLine.practiced()).isTrue();
        var inequalitiesLine = evolution.perSubtopic().get(1);
        assertThat(inequalitiesLine.deltaPoints()).contains(-20);
        assertThat(inequalitiesLine.level()).isEqualTo(MasteryLevel.LOW);
        assertThat(evolution.groupInitial()).isEqualTo(40.0);
        assertThat(evolution.groupCurrent()).isEqualTo(60.0);
        assertThat(evolution.groupDeltaPoints()).isEqualTo(20);
    }

    @Test
    void shouldReportNoActivityForACourseWithoutSolvedExercises() {
        var report = calculator.calculate(List.of(), List.of());

        assertThat(report.hasActivity()).isFalse();
        assertThat(report.accuracy().perStudent()).isEmpty();
        assertThat(report.accuracy().groupAccuracy()).isZero();
        assertThat(report.practice().totalSolved()).isZero();
        assertThat(report.practice().averagePerActiveStudent()).isZero();
        assertThat(report.evolution().perSubtopic()).isEmpty();
        assertThat(report.verification().perSubtopic()).isEmpty();
    }
}
