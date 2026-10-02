package com.kalibra.api.progress.application.internal.outboundservices.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kalibra.api.progress.domain.model.valueobjects.CourseId;
import com.kalibra.api.progress.domain.model.valueobjects.MasteryGapMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

// The cache is an optimisation: when Redis is down every read is a miss and writes are dropped,
// so the gap map is simply computed again.
@Service
public class RedisGapMapCacheService
        implements GapMapCacheService {

    private static final Logger log = LoggerFactory.getLogger(RedisGapMapCacheService.class);
    private static final String KEY_PREFIX = "kalibra:api:gap-map:";

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final Duration timeToLive;

    public RedisGapMapCacheService(StringRedisTemplate redis,
                                   ObjectMapper objectMapper,
                                   @Value("${kalibra.progress.gap-map-cache-ttl}") Duration timeToLive) {
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.timeToLive = timeToLive;
    }

    @Override
    public Optional<MasteryGapMap> find(CourseId courseId) {
        try {
            var cached = redis.opsForValue().get(keyOf(courseId));
            return cached == null
                    ? Optional.empty()
                    : Optional.of(objectMapper.readValue(cached, MasteryGapMap.class));
        } catch (JsonProcessingException | RuntimeException unavailable) {
            log.warn("Gap map cache read skipped: {}", unavailable.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void store(MasteryGapMap map) {
        try {
            redis.opsForValue().set(
                    keyOf(new CourseId(map.courseId())),
                    objectMapper.writeValueAsString(map),
                    timeToLive);
        } catch (JsonProcessingException | RuntimeException unavailable) {
            log.warn("Gap map cache write skipped: {}", unavailable.getMessage());
        }
    }

    @Override
    public void evict(CourseId courseId) {
        try {
            redis.delete(keyOf(courseId));
        } catch (RuntimeException unavailable) {
            log.warn("Gap map cache eviction skipped: {}", unavailable.getMessage());
        }
    }

    private String keyOf(CourseId courseId) {
        return KEY_PREFIX + courseId.value();
    }
}
