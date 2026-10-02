package com.kalibra.api.progress.domain.model.valueobjects;

public record IndicatorGuideEntry(
        IndicatorKind indicator,
        String whatItMeasures,
        String goodSignal
) {
}
