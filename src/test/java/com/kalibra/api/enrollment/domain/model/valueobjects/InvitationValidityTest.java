package com.kalibra.api.enrollment.domain.model.valueobjects;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class InvitationValidityTest {

    private final Instant sentAt = Instant.parse("2026-09-01T10:00:00Z");
    private final InvitationValidity validity = InvitationValidity.threeDaysFrom(sentAt);

    @Test
    void shouldExpireThreeDaysAfterSending() {
        assertThat(validity.expiresAt()).isEqualTo(sentAt.plus(Duration.ofDays(3)));
    }

    @Test
    void shouldStillBeValidJustBeforeTheThirdDay() {
        assertThat(validity.hasExpired(validity.expiresAt().minusSeconds(1))).isFalse();
    }

    @Test
    void shouldHaveExpiredWhenTheThirdDayIsReached() {
        assertThat(validity.hasExpired(validity.expiresAt())).isTrue();
    }
}
