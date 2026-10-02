package com.kalibra.api.progress.domain.model.valueobjects;

public record IndicatorExportRow(
        AnonymousStudentCode studentCode,
        String subtopicName,
        int solved,
        double accuracy,
        double initialMastery,
        double currentMastery,
        double verificationApprovalRate
) {
}
