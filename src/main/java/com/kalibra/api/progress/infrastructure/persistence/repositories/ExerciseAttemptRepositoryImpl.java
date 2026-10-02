package com.kalibra.api.progress.infrastructure.persistence.repositories;

import com.kalibra.api.progress.domain.model.aggregates.ExerciseAttempt;
import com.kalibra.api.progress.domain.model.valueobjects.AnswerResult;
import com.kalibra.api.progress.domain.model.valueobjects.CourseId;
import com.kalibra.api.progress.domain.model.valueobjects.ExerciseAttemptPage;
import com.kalibra.api.progress.domain.model.valueobjects.Pagination;
import com.kalibra.api.progress.domain.model.valueobjects.SubtopicId;
import com.kalibra.api.progress.domain.repositories.ExerciseAttemptRepository;
import com.kalibra.api.progress.infrastructure.persistence.entities.ExerciseAttemptJpaEntity;
import com.kalibra.api.progress.infrastructure.persistence.transform.ExerciseAttemptJpaMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class ExerciseAttemptRepositoryImpl
        implements ExerciseAttemptRepository {

    private final ExerciseAttemptJpaRepository jpaRepository;
    private final ExerciseAttemptJpaMapper mapper;

    public ExerciseAttemptRepositoryImpl(
            ExerciseAttemptJpaRepository jpaRepository,
            ExerciseAttemptJpaMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public ExerciseAttempt save(ExerciseAttempt attempt) {
        return mapper.toDomain(
                jpaRepository.saveAndFlush(mapper.toEntity(attempt))
        );
    }

    @Override
    public List<ExerciseAttempt> findAllByHolderIdAndCourseId(
            String holderId,
            CourseId courseId) {
        return jpaRepository
                .findAllByHolderIdAndCourseIdOrderByAnsweredAtDesc(
                        holderId,
                        courseId.value())
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public ExerciseAttemptPage findAllByHolderIdAndCourseId(
            String holderId,
            CourseId courseId,
            Pagination pagination) {
        return toPage(jpaRepository.findAllByHolderIdAndCourseId(
                holderId,
                courseId.value(),
                newestFirst(pagination)
        ));
    }

    @Override
    public ExerciseAttemptPage findAllByHolderIdAndCourseIdAndResult(
            String holderId,
            CourseId courseId,
            AnswerResult result,
            Pagination pagination) {
        return toPage(jpaRepository.findAllByHolderIdAndCourseIdAndResult(
                holderId,
                courseId.value(),
                result.name(),
                newestFirst(pagination)
        ));
    }

    @Override
    public long countByHolderIdAndCourseIdAndResult(
            String holderId,
            CourseId courseId,
            AnswerResult result) {
        return jpaRepository.countByHolderIdAndCourseIdAndResult(
                holderId,
                courseId.value(),
                result.name()
        );
    }

    @Override
    public Optional<ExerciseAttempt> findFirstByHolderIdAndSubtopicIdOrderByAnsweredAtDesc(
            String holderId,
            SubtopicId subtopicId) {
        return jpaRepository
                .findFirstByHolderIdAndSubtopicIdOrderByAnsweredAtDesc(
                        holderId,
                        subtopicId.value())
                .map(mapper::toDomain);
    }

    @Override
    public List<ExerciseAttempt> findAllByCourseId(CourseId courseId) {
        return jpaRepository
                .findAllByCourseId(courseId.value())
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    private Pageable newestFirst(Pagination pagination) {
        return PageRequest.of(
                pagination.page(),
                pagination.size(),
                Sort.by(Sort.Direction.DESC, "answeredAt").and(Sort.by("id"))
        );
    }

    private ExerciseAttemptPage toPage(Page<ExerciseAttemptJpaEntity> page) {
        var items = page.getContent()
                .stream()
                .map(mapper::toDomain)
                .toList();
        return new ExerciseAttemptPage(
                items,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
