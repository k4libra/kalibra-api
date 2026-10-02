package com.kalibra.api.progress.application.internal.outboundservices.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kalibra.api.progress.domain.model.valueobjects.CourseId;
import com.kalibra.api.progress.domain.model.valueobjects.MasteryGapMap;
import com.kalibra.api.progress.domain.model.valueobjects.MasteryLevel;
import com.kalibra.api.progress.domain.model.valueobjects.MasteryProbability;
import com.kalibra.api.progress.domain.model.valueobjects.StudentMasteryCell;
import com.kalibra.api.progress.domain.model.valueobjects.SubtopicGapLine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisGapMapCacheServiceTest {

    @Mock
    StringRedisTemplate redis;

    @Mock
    ValueOperations<String, String> values;

    private final ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();
    private final CourseId courseId = new CourseId(UUID.randomUUID());
    private final Duration timeToLive = Duration.ofMinutes(10);

    private RedisGapMapCacheService cache;

    @BeforeEach
    void setUp() {
        cache = new RedisGapMapCacheService(redis, objectMapper, timeToLive);
    }

    @Test
    void shouldStoreTheGapMapWithItsExpirationAndReadItBack() {
        var subtopicId = UUID.randomUUID();
        var map = new MasteryGapMap(courseId.value(), true,
                List.of(new SubtopicGapLine(subtopicId, "Equations", 41.5, 1, 1, 0, 1, 1)),
                List.of(new StudentMasteryCell(UUID.randomUUID(), "ana@kalibra.pe", subtopicId,
                                Optional.of(new MasteryProbability(0.41)), MasteryLevel.MEDIUM),
                        new StudentMasteryCell(UUID.randomUUID(), "luis@kalibra.pe", subtopicId,
                                Optional.empty(), MasteryLevel.NO_DATA)));
        var stored = ArgumentCaptor.forClass(String.class);
        when(redis.opsForValue()).thenReturn(values);

        cache.store(map);

        verify(values).set(eq("kalibra:api:gap-map:" + courseId.value()), stored.capture(), eq(timeToLive));
        when(values.get("kalibra:api:gap-map:" + courseId.value())).thenReturn(stored.getValue());
        assertThat(cache.find(courseId)).contains(map);
    }

    @Test
    void shouldMissWhenTheCourseHasNoCachedMap() {
        when(redis.opsForValue()).thenReturn(values);

        assertThat(cache.find(courseId)).isEmpty();
    }

    @Test
    void shouldEvictTheMapOfTheCourse() {
        cache.evict(courseId);

        verify(redis).delete("kalibra:api:gap-map:" + courseId.value());
    }

    @Test
    void shouldBehaveAsAMissWhenRedisIsDown() {
        when(redis.opsForValue()).thenThrow(new RedisConnectionFailureException("down"));
        when(redis.delete("kalibra:api:gap-map:" + courseId.value())).thenThrow(new RedisConnectionFailureException("down"));

        assertThat(cache.find(courseId)).isEmpty();
        assertThatCode(() -> cache.store(new MasteryGapMap(courseId.value(), false, List.of(), List.of()))).doesNotThrowAnyException();
        assertThatCode(() -> cache.evict(courseId)).doesNotThrowAnyException();
    }
}
