package com.kalibra.api.progress.application.internal.outboundservices.cache;

import com.kalibra.api.progress.domain.model.valueobjects.CourseId;
import com.kalibra.api.progress.domain.model.valueobjects.MasteryGapMap;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryGapMapCacheServiceTest {

    private final MutableClock clock = new MutableClock(Instant.parse("2026-11-15T10:00:00Z"));
    private final InMemoryGapMapCacheService cache = new InMemoryGapMapCacheService(Duration.ofMinutes(10), clock);
    private final CourseId courseId = new CourseId(UUID.randomUUID());
    private final MasteryGapMap map = new MasteryGapMap(courseId.value(), true, List.of(), List.of());

    @Test
    void returnsTheStoredMapWithinItsTimeToLive() {
        cache.store(map);
        clock.advance(Duration.ofMinutes(9));

        assertThat(cache.find(courseId)).contains(map);
    }

    @Test
    void treatsAnExpiredMapAsAMiss() {
        cache.store(map);
        clock.advance(Duration.ofMinutes(10));

        assertThat(cache.find(courseId)).isEmpty();
    }

    @Test
    void evictionForcesTheNextReadToRecompute() {
        cache.store(map);
        cache.evict(courseId);

        assertThat(cache.find(courseId)).isEmpty();
        assertThat(cache.find(new CourseId(UUID.randomUUID()))).isEmpty();
    }

    private static final class MutableClock extends Clock {
        private Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
