package com.kalibra.api.progress.application.internal.outboundservices.cache;

import com.kalibra.api.progress.domain.model.valueobjects.CourseId;
import com.kalibra.api.progress.domain.model.valueobjects.MasteryGapMap;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RedisGapMapCacheService
        implements GapMapCacheService {

    private final Map<CourseId, MasteryGapMap> cache =
            new ConcurrentHashMap<>();

    @Override
    public Optional<MasteryGapMap> find(CourseId courseId) {
        return Optional.ofNullable(cache.get(courseId));
    }

    @Override
    public void store(MasteryGapMap map) {
        cache.put(
                new CourseId(map.courseId()),
                map
        );
    }

    @Override
    public void evict(CourseId courseId) {
        cache.remove(courseId);
    }
}