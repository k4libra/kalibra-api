package com.kalibra.api.progress.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

public record IndicatorGuideEntryResource(
        @Schema(allowableValues = {"ACCURACY", "PRACTICE", "MASTERY_EVOLUTION", "VERIFICATION_APPROVAL"})
        String indicator,
        String whatItMeasures,
        String goodSignal
) {
}
