package com.kalibra.api.curriculum.domain.model.valueobjects;

import java.util.Optional;

public record VerificationOutcome(
        VerificationVerdict verdict,
        boolean correctnessPassed,
        boolean difficultyPassed,
        Optional<String> rejectionReason,
        boolean usedFallback
) {

    public VerificationOutcome {
        if (verdict == null) {
            throw new IllegalArgumentException("Verification verdict cannot be null");
        }
        if (rejectionReason == null) {
            rejectionReason = Optional.empty();
        }
        if (verdict == VerificationVerdict.APPROVED && !(correctnessPassed && difficultyPassed)) {
            throw new IllegalArgumentException("An approved exercise must pass both the correctness and the difficulty checks");
        }
    }

    public boolean isApproved() {
        return verdict == VerificationVerdict.APPROVED;
    }
}
