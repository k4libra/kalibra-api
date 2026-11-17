package com.kalibra.api.progress.application.internal.outboundservices.cache;

import com.kalibra.api.progress.domain.model.valueobjects.CourseId;
import com.kalibra.api.progress.domain.model.valueobjects.MasteryGapMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// In-process cache of computed gap maps. It is only an optimisation: a miss recomputes the map,
// and the answer handler evicts the course after each mastery update. Every API instance keeps
// its own copy, so with several instances another one may serve a map up to the TTL old.
@Service
public class InMemoryGapMapCacheService
        implements GapMapCacheService {

    private final Map<UUID, Entry> entries = new ConcurrentHashMap<>();
    private final Duration timeToLive;
    private final Clock clock;

    @Autowired
    public InMemoryGapMapCacheService(@Value("${kalibra.progress.gap-map-cache-ttl}") Duration timeToLive) {
        this(timeToLive, Clock.systemUTC());
    }

    InMemoryGapMapCacheService(Duration timeToLive, Clock clock) {
        this.timeToLive = timeToLive;
        this.clock = clock;
    }

    @Override
    public Optional<MasteryGapMap> find(CourseId courseId) {
        var entry = entries.get(courseId.value());
        if (entry == null) {
            return Optional.empty();
        }
        if (!clock.instant().isBefore(entry.expiresAt())) {
            entries.remove(courseId.value(), entry);
            return Optional.empty();
        }
        return Optional.of(entry.map());
    }

    @Override
    public void store(MasteryGapMap map) {
        entries.put(map.courseId(), new Entry(map, clock.instant().plus(timeToLive)));
    }

    @Override
    public void evict(CourseId courseId) {
        entries.remove(courseId.value());
    }

    private record Entry(MasteryGapMap map, Instant expiresAt) {
    }
}
