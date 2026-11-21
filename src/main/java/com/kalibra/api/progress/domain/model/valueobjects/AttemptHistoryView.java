package com.kalibra.api.progress.domain.model.valueobjects;

import java.util.List;

public record AttemptHistoryView(
        int allCount,
        int correctCount,
        int incorrectCount,
        AttemptResultFilter activeFilter,
        List<AttemptHistoryLine> lines,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public AttemptHistoryView {
        lines = List.copyOf(lines);
    }
}
