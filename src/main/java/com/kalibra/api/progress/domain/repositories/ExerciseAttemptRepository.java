package com.kalibra.api.progress.domain.repositories;

import com.kalibra.api.progress.domain.model.aggregates.ExerciseAttempt;
import com.kalibra.api.progress.domain.model.valueobjects.AnswerResult;
import com.kalibra.api.progress.domain.model.valueobjects.CourseId;
import com.kalibra.api.progress.domain.model.valueobjects.ExerciseId;
import com.kalibra.api.progress.domain.model.valueobjects.ExerciseAttemptPage;
import com.kalibra.api.progress.domain.model.valueobjects.Pagination;
import com.kalibra.api.progress.domain.model.valueobjects.SubtopicId;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface ExerciseAttemptRepository {

    ExerciseAttempt save(ExerciseAttempt attempt);

    Set<ExerciseId> findAnsweredExerciseIdsByHolderIdAndSubtopicId(String holderId, SubtopicId subtopicId);

    Optional<ExerciseAttempt> findFirstByHolderIdAndCourseIdOrderByAnsweredAtDesc(String holderId, CourseId courseId);

    List<ExerciseAttempt> findAllByHolderIdAndCourseId(
            String holderId,
            CourseId courseId
    );

    ExerciseAttemptPage findAllByHolderIdAndCourseId(
            String holderId,
            CourseId courseId,
            Pagination pagination
    );

    ExerciseAttemptPage findAllByHolderIdAndCourseIdAndResult(
            String holderId,
            CourseId courseId,
            AnswerResult result,
            Pagination pagination
    );

    long countByHolderIdAndCourseIdAndResult(
            String holderId,
            CourseId courseId,
            AnswerResult result
    );

    Optional<ExerciseAttempt> findFirstByHolderIdAndSubtopicIdOrderByAnsweredAtDesc(
            String holderId,
            SubtopicId subtopicId
    );

    List<ExerciseAttempt> findAllByCourseId(CourseId courseId);
}
