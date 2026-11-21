package com.kalibra.api.curriculum.infrastructure.persistence.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class VerificationOutcomeEmbeddable {

    @Column(nullable = false, length = 10)
    private String verdict;

    @Column(name = "correctness_passed", nullable = false)
    private boolean correctnessPassed;

    @Column(name = "difficulty_passed", nullable = false)
    private boolean difficultyPassed;

    @Column(name = "rejection_reason", length = 1000)
    private String rejectionReason;

    @Column(name = "used_fallback", nullable = false)
    private boolean usedFallback;

    // public: required by MapStruct, which generates its mapper impl in a different package.
    public VerificationOutcomeEmbeddable() {
    }

    public String getVerdict() {
        return verdict;
    }

    public void setVerdict(String verdict) {
        this.verdict = verdict;
    }

    public boolean isCorrectnessPassed() {
        return correctnessPassed;
    }

    public void setCorrectnessPassed(boolean correctnessPassed) {
        this.correctnessPassed = correctnessPassed;
    }

    public boolean isDifficultyPassed() {
        return difficultyPassed;
    }

    public void setDifficultyPassed(boolean difficultyPassed) {
        this.difficultyPassed = difficultyPassed;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public boolean isUsedFallback() {
        return usedFallback;
    }

    public void setUsedFallback(boolean usedFallback) {
        this.usedFallback = usedFallback;
    }
}
