package com.kalibra.api.progress.domain.model.valueobjects;

import java.util.List;

public record IndicatorGuide(List<IndicatorGuideEntry> entries) {

    public IndicatorGuide {
        entries = List.copyOf(entries);
    }

    public static IndicatorGuide standard() {
        return new IndicatorGuide(List.of(
                new IndicatorGuideEntry(
                        IndicatorKind.ACCURACY,
                        "Out of all the answers your students sent, how many were right. "
                                + "It is shown for each student who has practiced and for the course as a whole.",
                        "A percentage that stays high or grows as they practice. "
                                + "A student well below the course is someone to check on."),
                new IndicatorGuideEntry(
                        IndicatorKind.PRACTICE,
                        "How many exercises your students have solved: in total, in each subtopic "
                                + "and on average per student who practices.",
                        "Most enrolled students practice and every subtopic has solved exercises. "
                                + "A subtopic with very few is one they are avoiding."),
                new IndicatorGuideEntry(
                        IndicatorKind.MASTERY_EVOLUTION,
                        "How much your students' level in each subtopic has changed "
                                + "since they started practicing it, compared with their level today.",
                        "The level today is above the starting level. "
                                + "A subtopic that goes down or does not move needs reinforcement in class."),
                new IndicatorGuideEntry(
                        IndicatorKind.VERIFICATION_APPROVAL,
                        "Out of all the exercises created automatically for your course, how many passed "
                                + "the review of correctness and difficulty. Only those reach your students.",
                        "A high percentage. A low one in a subtopic usually means "
                                + "its material is scarce or hard to read, so uploading better material helps.")
        ));
    }
}
