package com.kalibra.api.shared.contracts.curriculum;

public record ExplanationStep(String title, String detail, String kind) {
    public ExplanationStep {
        if (detail == null || detail.isBlank()) {
            throw new IllegalArgumentException("Explanation step detail cannot be blank");
        }
        if (!"TEXT".equals(kind) && !"CODE".equals(kind) && !"FORMULA".equals(kind)) {
            throw new IllegalArgumentException("Explanation step kind must be TEXT, CODE or FORMULA");
        }
    }
}
