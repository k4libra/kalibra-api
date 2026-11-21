package com.kalibra.api.progress.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record MasteryEvolutionResource(
        @Schema(description = "Average of the first mastery estimates of the group, as a percentage 0..100")
        double groupInitial,
        @Schema(description = "Average of the current mastery estimates of the group, as a percentage 0..100")
        double groupCurrent,
        int groupDeltaPoints,
        List<SubtopicEvolutionResource> perSubtopic
) {
}
