package com.kalibra.api.curriculum.infrastructure.persistence.repositories;

import com.kalibra.api.curriculum.domain.model.aggregates.Course;
import com.kalibra.api.curriculum.domain.model.valueobjects.CourseId;
import com.kalibra.api.curriculum.domain.repositories.CourseRepository;
import com.kalibra.api.curriculum.infrastructure.persistence.transform.CourseJpaMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class CourseRepositoryImpl implements CourseRepository {

    private final CourseJpaRepository jpaRepository;
    private final CourseJpaMapper mapper;

    public CourseRepositoryImpl(CourseJpaRepository jpaRepository, CourseJpaMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Course save(Course course) {
        var entity = mapper.toEntity(course);
        var saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Course> findById(CourseId id) {
        return jpaRepository.findById(id.value()).map(mapper::toDomain);
    }

    @Override
    public Optional<Course> findByIdAndHolderId(CourseId id, String holderId) {
        return jpaRepository.findByIdAndHolderId(id.value(), holderId).map(mapper::toDomain);
    }

    @Override
    public List<Course> findAllByHolderId(String holderId) {
        return jpaRepository.findAllByHolderId(holderId).stream().map(mapper::toDomain).toList();
    }
}
