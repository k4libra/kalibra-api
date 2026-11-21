package com.kalibra.api.progress.application.internal.outboundservices.cache;

import com.kalibra.api.progress.domain.model.valueobjects.CourseId;
import com.kalibra.api.progress.domain.model.valueobjects.MasteryGapMap;

import java.util.Optional;

public interface GapMapCacheService {

    Optional<MasteryGapMap> find(CourseId courseId);

    void store(MasteryGapMap map);

    void evict(CourseId courseId);
}