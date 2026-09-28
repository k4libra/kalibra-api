package com.kalibra.api.progress.infrastructure.persistence.repositories;

import com.kalibra.api.progress.domain.model.aggregates.SubtopicMastery;
import com.kalibra.api.progress.domain.model.valueobjects.CourseId;
import com.kalibra.api.progress.domain.model.valueobjects.SubtopicId;
import com.kalibra.api.progress.domain.repositories.SubtopicMasteryRepository;
import com.kalibra.api.progress.infrastructure.persistence.transform.SubtopicMasteryJpaMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class SubtopicMasteryRepositoryImpl
        implements SubtopicMasteryRepository {

    private final SubtopicMasteryJpaRepository jpaRepository;
    private final SubtopicMasteryJpaMapper mapper;

    public SubtopicMasteryRepositoryImpl(
            SubtopicMasteryJpaRepository jpaRepository,
            SubtopicMasteryJpaMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public SubtopicMastery save(SubtopicMastery mastery) {
        return mapper.toDomain(
                jpaRepository.save(mapper.toEntity(mastery))
        );
    }

    @Override
    public Optional<SubtopicMastery> findByHolderIdAndSubtopicId(
            String holderId,
            SubtopicId subtopicId) {

        return jpaRepository
                .findByHolderIdAndSubtopicId(
                        holderId,
                        subtopicId.value())
                .map(mapper::toDomain);
    }

    @Override
    public List<SubtopicMastery> findAllByHolderIdAndCourseId(
            String holderId,
            CourseId courseId) {

        return jpaRepository
                .findAllByHolderIdAndCourseId(
                        holderId,
                        courseId.value())
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<SubtopicMastery> findAllByCourseId(CourseId courseId) {

        return jpaRepository
                .findAllByCourseId(courseId.value())
                .stream()
                .map(mapper::toDomain)
                .toList();
    }
}