package com.kalibra.api.curriculum.infrastructure.persistence.repositories;

import com.kalibra.api.curriculum.domain.model.aggregates.GeneratedExercise;
import com.kalibra.api.curriculum.domain.model.valueobjects.CourseId;
import com.kalibra.api.curriculum.domain.model.valueobjects.ExerciseId;
import com.kalibra.api.curriculum.domain.model.valueobjects.GeneratedExercisePage;
import com.kalibra.api.curriculum.domain.model.valueobjects.Pagination;
import com.kalibra.api.curriculum.domain.model.valueobjects.SubtopicId;
import com.kalibra.api.curriculum.domain.repositories.GeneratedExerciseRepository;
import com.kalibra.api.curriculum.infrastructure.persistence.entities.GeneratedExerciseJpaEntity;
import com.kalibra.api.curriculum.infrastructure.persistence.transform.GeneratedExerciseJpaMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class GeneratedExerciseRepositoryImpl implements GeneratedExerciseRepository {

    private final GeneratedExerciseJpaRepository jpaRepository;
    private final GeneratedExerciseJpaMapper mapper;

    public GeneratedExerciseRepositoryImpl(GeneratedExerciseJpaRepository jpaRepository, GeneratedExerciseJpaMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public GeneratedExercise save(GeneratedExercise exercise) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(exercise)));
    }

    @Override
    public List<GeneratedExercise> saveAll(List<GeneratedExercise> exercises) {
        var entities = exercises.stream().map(mapper::toEntity).toList();
        return jpaRepository.saveAll(entities).stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<GeneratedExercise> findById(ExerciseId id) {
        return jpaRepository.findById(id.value()).map(mapper::toDomain);
    }

    @Override
    public List<GeneratedExercise> findAllByCourseId(CourseId courseId) {
        return jpaRepository.findAllByCourseId(courseId.value()).stream().map(mapper::toDomain).toList();
    }

    @Override
    public GeneratedExercisePage findAllByCourseId(CourseId courseId, Pagination pagination) {
        return toPage(jpaRepository.findAllByCourseId(courseId.value(), newestFirst(pagination)));
    }

    @Override
    public GeneratedExercisePage findAllByCourseIdAndSubtopicId(CourseId courseId, SubtopicId subtopicId, Pagination pagination) {
        return toPage(jpaRepository.findAllByCourseIdAndSubtopicId(courseId.value(), subtopicId.value(), newestFirst(pagination)));
    }

    @Override
    public List<GeneratedExercise> findAllByCourseIdIn(List<CourseId> courseIds) {
        if (courseIds.isEmpty()) {
            return List.of();
        }
        var ids = courseIds.stream().map(CourseId::value).toList();
        return jpaRepository.findAllByCourseIdIn(ids).stream().map(mapper::toDomain).toList();
    }

    private Pageable newestFirst(Pagination pagination) {
        return PageRequest.of(pagination.page(), pagination.size(),
                Sort.by(Sort.Direction.DESC, "generatedAt").and(Sort.by("id")));
    }

    private GeneratedExercisePage toPage(Page<GeneratedExerciseJpaEntity> page) {
        var items = page.getContent().stream().map(mapper::toDomain).toList();
        return new GeneratedExercisePage(items, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
