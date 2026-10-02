package com.kalibra.api.progress.interfaces.rest.resources;

import java.util.List;

public record PracticeIndicatorResource(
        int totalSolved,
        double averagePerActiveStudent,
        int activeStudents,
        int enrolledStudents,
        List<SubtopicPracticeResource> perSubtopic
) {
}
