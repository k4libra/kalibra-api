package com.kalibra.api.progress.domain.model.valueobjects;

public enum MasteryLevel {
    LOW,
    MEDIUM,
    HIGH,
    NO_DATA;

    private static final double MEDIUM_FROM = 40;
    private static final double HIGH_ABOVE = 70;

    public static MasteryLevel ofPercentage(double percentage) {
        if (percentage < MEDIUM_FROM) {
            return LOW;
        }
        return percentage > HIGH_ABOVE ? HIGH : MEDIUM;
    }
}
