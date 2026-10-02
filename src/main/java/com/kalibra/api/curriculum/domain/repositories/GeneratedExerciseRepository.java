package com.kalibra.api.curriculum.domain.repositories;

import com.kalibra.api.curriculum.domain.model.aggregates.GeneratedExercise;
import com.kalibra.api.curriculum.domain.model.valueobjects.CourseId;
import com.kalibra.api.curriculum.domain.model.valueobjects.ExerciseId;
import com.kalibra.api.curriculum.domain.model.valueobjects.GeneratedExercisePage;
import com.kalibra.api.curriculum.domain.model.valueobjects.Pagination;
import com.kalibra.api.curriculum.domain.model.valueobjects.SubtopicId;

import java.util.List;
import java.util.Optional;

public interface GeneratedExerciseRepository {

    GeneratedExercise save(GeneratedExercise exercise);

    List<GeneratedExercise> saveAll(List<GeneratedExercise> exercises);

    Optional<GeneratedExercise> findById(ExerciseId id);

    List<GeneratedExercise> findAllByCourseId(CourseId courseId);

    GeneratedExercisePage findAllByCourseId(CourseId courseId, Pagination pagination);

    GeneratedExercisePage findAllByCourseIdAndSubtopicId(CourseId courseId, SubtopicId subtopicId, Pagination pagination);

    List<GeneratedExercise> findAllByCourseIdIn(List<CourseId> courseIds);
}
