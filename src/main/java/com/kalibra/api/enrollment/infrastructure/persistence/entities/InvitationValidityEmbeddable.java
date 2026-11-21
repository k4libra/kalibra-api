package com.kalibra.api.enrollment.infrastructure.persistence.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.time.Instant;

@Embeddable
public class InvitationValidityEmbeddable {

    @Column(nullable = false)
    private Instant sentAt;

    @Column(nullable = false)
    private Instant expiresAt;

    public InvitationValidityEmbeddable() {
    }

    public InvitationValidityEmbeddable(Instant sentAt, Instant expiresAt) {
        this.sentAt = sentAt;
        this.expiresAt = expiresAt;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public void setSentAt(Instant sentAt) {
        this.sentAt = sentAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }
}