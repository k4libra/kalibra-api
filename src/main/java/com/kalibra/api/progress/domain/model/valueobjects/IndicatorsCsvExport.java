package com.kalibra.api.progress.domain.model.valueobjects;

import java.util.List;

public record IndicatorsCsvExport(
        String fileName,
        List<IndicatorExportRow> rows
) {

    public IndicatorsCsvExport {
        rows = List.copyOf(rows);
    }
}
