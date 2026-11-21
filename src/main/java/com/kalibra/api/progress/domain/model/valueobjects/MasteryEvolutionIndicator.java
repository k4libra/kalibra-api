package com.kalibra.api.progress.domain.model.valueobjects;

import java.util.List;

public record MasteryEvolutionIndicator(
        double groupInitial,
        double groupCurrent,
        int groupDeltaPoints,
        List<SubtopicEvolutionLine> perSubtopic
) {

    public MasteryEvolutionIndicator {
        perSubtopic = List.copyOf(perSubtopic);
    }
}
