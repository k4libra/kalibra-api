package com.kalibra.api.curriculum.domain.repositories;

import com.kalibra.api.curriculum.domain.model.aggregates.Course;
import com.kalibra.api.curriculum.domain.model.valueobjects.CourseId;

import java.util.List;
import java.util.Optional;

public interface CourseRepository {

    Course save(Course course);

    Optional<Course> findById(CourseId id);

    Optional<Course> findByIdAndHolderId(CourseId id, String holderId);

    List<Course> findAllByHolderId(String holderId);
}
