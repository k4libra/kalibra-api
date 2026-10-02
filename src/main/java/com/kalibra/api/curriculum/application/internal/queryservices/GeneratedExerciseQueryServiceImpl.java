package com.kalibra.api.curriculum.application.internal.queryservices;

import com.kalibra.api.curriculum.domain.exceptions.CourseNotOwnedByTeacherException;
import com.kalibra.api.curriculum.domain.model.aggregates.Course;
import com.kalibra.api.curriculum.domain.model.aggregates.GeneratedExercise;
import com.kalibra.api.curriculum.domain.model.queries.GetExerciseCatalogByHolderIdQuery;
import com.kalibra.api.curriculum.domain.model.queries.GetGeneratedExerciseByIdQuery;
import com.kalibra.api.curriculum.domain.model.queries.GetGeneratedExercisesByCourseQuery;
import com.kalibra.api.curriculum.domain.model.queries.GetVerificationApprovalByCourseQuery;
import com.kalibra.api.curriculum.domain.model.valueobjects.CourseExerciseCatalog;
import com.kalibra.api.curriculum.domain.model.valueobjects.GeneratedExercisePage;
import com.kalibra.api.curriculum.domain.model.valueobjects.SubtopicExerciseCount;
import com.kalibra.api.curriculum.domain.model.valueobjects.VerificationApprovalReport;
import com.kalibra.api.curriculum.domain.repositories.CourseRepository;
import com.kalibra.api.curriculum.domain.repositories.GeneratedExerciseRepository;
import com.kalibra.api.curriculum.domain.services.GeneratedExerciseQueryService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class GeneratedExerciseQueryServiceImpl implements GeneratedExerciseQueryService {

    private final GeneratedExerciseRepository generatedExerciseRepository;
    private final CourseRepository courseRepository;

    public GeneratedExerciseQueryServiceImpl(GeneratedExerciseRepository generatedExerciseRepository,
                                             CourseRepository courseRepository) {
        this.generatedExerciseRepository = generatedExerciseRepository;
        this.courseRepository = courseRepository;
    }

    @Override
    public GeneratedExercisePage handle(GetGeneratedExercisesByCourseQuery query) {
        courseRepository.findByIdAndHolderId(query.courseId(), query.holderId())
                .orElseThrow(() -> new CourseNotOwnedByTeacherException(query.courseId().value()));
        return query.subtopicId()
                .map(subtopicId -> generatedExerciseRepository
                        .findAllByCourseIdAndSubtopicId(query.courseId(), subtopicId, query.pagination()))
                .orElseGet(() -> generatedExerciseRepository.findAllByCourseId(query.courseId(), query.pagination()));
    }

    @Override
    public List<CourseExerciseCatalog> handle(GetExerciseCatalogByHolderIdQuery query) {
        var courses = courseRepository.findAllByHolderId(query.holderId());
        var exercises = generatedExerciseRepository.findAllByCourseIdIn(courses.stream().map(Course::getId).toList());
        return courses.stream()
                .map(course -> new CourseExerciseCatalog(course.getId().value(), course.getName(),
                        countBySubtopic(course, exercises)))
                .toList();
    }

    @Override
    public VerificationApprovalReport handle(GetVerificationApprovalByCourseQuery query) {
        var bySubtopic = courseRepository.findById(query.courseId())
                .map(course -> countBySubtopic(course, generatedExerciseRepository.findAllByCourseId(query.courseId())))
                .orElse(List.of()).stream()
                .filter(count -> count.generated() > 0)
                .toList();
        var generated = bySubtopic.stream().mapToInt(SubtopicExerciseCount::generated).sum();
        var approved = bySubtopic.stream().mapToInt(SubtopicExerciseCount::approved).sum();
        var approvalRate = generated == 0 ? 0 : approved * 100.0 / generated;
        return new VerificationApprovalReport(generated, approved, generated - approved, approvalRate, bySubtopic);
    }

    @Override
    public Optional<GeneratedExercise> handle(GetGeneratedExerciseByIdQuery query) {
        return generatedExerciseRepository.findById(query.exerciseId());
    }

    private List<SubtopicExerciseCount> countBySubtopic(Course course, List<GeneratedExercise> exercises) {
        return course.getSubtopics().stream()
                .map(subtopic -> {
                    var ofSubtopic = exercises.stream()
                            .filter(exercise -> exercise.getCourseId().equals(course.getId())
                                    && exercise.getSubtopicId().equals(subtopic.getId()))
                            .toList();
                    var approved = (int) ofSubtopic.stream()
                            .filter(exercise -> exercise.getVerification().isApproved())
                            .count();
                    return new SubtopicExerciseCount(subtopic.getId().value(), subtopic.getName(),
                            ofSubtopic.size(), approved, ofSubtopic.size() - approved);
                })
                .toList();
    }
}
