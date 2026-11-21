package com.kalibra.api.progress.domain.repositories;

import com.kalibra.api.progress.domain.model.aggregates.SubtopicMastery;
import com.kalibra.api.progress.domain.model.valueobjects.CourseId;
import com.kalibra.api.progress.domain.model.valueobjects.SubtopicId;

import java.util.List;
import java.util.Optional;

public interface SubtopicMasteryRepository {

    SubtopicMastery save(SubtopicMastery mastery);

    Optional<SubtopicMastery> findByHolderIdAndSubtopicId(
            String holderId,
            SubtopicId subtopicId
    );

    List<SubtopicMastery> findAllByHolderIdAndCourseId(
            String holderId,
            CourseId courseId
    );

    List<SubtopicMastery> findAllByCourseId(CourseId courseId);
}