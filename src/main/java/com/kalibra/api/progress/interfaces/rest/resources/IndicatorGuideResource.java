package com.kalibra.api.progress.interfaces.rest.resources;

import java.util.List;

public record IndicatorGuideResource(
        List<IndicatorGuideEntryResource> entries
) {
}
