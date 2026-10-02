package com.kalibra.api.progress.domain.model.valueobjects;

import java.util.List;

public record PracticeIndicator(
        int totalSolved,
        double averagePerActiveStudent,
        int activeStudents,
        int enrolledStudents,
        List<SubtopicPracticeLine> perSubtopic
) {

    public PracticeIndicator {
        perSubtopic = List.copyOf(perSubtopic);
    }
}
