package com.kalibra.api.curriculum.application.internal.queryservices;

import com.kalibra.api.curriculum.domain.exceptions.CourseNotFoundException;
import com.kalibra.api.curriculum.domain.model.aggregates.Course;
import com.kalibra.api.curriculum.domain.model.entities.Subtopic;
import com.kalibra.api.curriculum.domain.model.queries.GetAllCoursesQuery;
import com.kalibra.api.curriculum.domain.model.queries.GetCourseByIdQuery;
import com.kalibra.api.curriculum.domain.model.queries.GetCoursesByHolderIdQuery;
import com.kalibra.api.curriculum.domain.model.queries.GetLockedSubtopicIdsByCourseQuery;
import com.kalibra.api.curriculum.domain.model.queries.GetPracticableSubtopicsQuery;
import com.kalibra.api.curriculum.domain.model.valueobjects.IngestionStatus;
import com.kalibra.api.curriculum.domain.model.valueobjects.SubtopicId;
import com.kalibra.api.curriculum.domain.repositories.CourseRepository;
import com.kalibra.api.curriculum.domain.repositories.CurricularMaterialRepository;
import com.kalibra.api.curriculum.domain.services.CourseQueryService;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class CourseQueryServiceImpl implements CourseQueryService {

    private final CourseRepository courseRepository;
    private final CurricularMaterialRepository curricularMaterialRepository;

    public CourseQueryServiceImpl(CourseRepository courseRepository,
                                  CurricularMaterialRepository curricularMaterialRepository) {
        this.courseRepository = courseRepository;
        this.curricularMaterialRepository = curricularMaterialRepository;
    }

    @Override
    public List<Course> handle(GetCoursesByHolderIdQuery query) {
        return courseRepository.findAllByHolderId(query.holderId());
    }

    @Override
    public List<Course> handle(GetAllCoursesQuery query) {
        return courseRepository.findAll();
    }

    @Override
    public Optional<Course> handle(GetCourseByIdQuery query) {
        return courseRepository.findById(query.courseId());
    }

    @Override
    public List<Subtopic> handle(
            GetPracticableSubtopicsQuery query) {
        return courseRepository.findById(query.courseId()).stream()
                .flatMap(course -> course.getSubtopics().stream())
                .filter(Subtopic::isActive)
                .filter(subtopic -> !curricularMaterialRepository.findAllByCourseIdAndSubtopicIdAndStatus(
                        query.courseId(), subtopic.getId(),
                        IngestionStatus.READY).isEmpty())
                .sorted(Comparator.comparingInt(Subtopic::getDisplayOrder))
                .toList();
    }

    @Override
    public Set<SubtopicId> handle(GetLockedSubtopicIdsByCourseQuery query) {
        courseRepository.findByIdAndHolderId(query.courseId(), query.holderId())
                .orElseThrow(() -> new CourseNotFoundException(query.courseId().value()));
        return curricularMaterialRepository.findMaterialSubtopicIdsByCourseId(query.courseId());
    }
}
