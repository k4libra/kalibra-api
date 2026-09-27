package com.kalibra.api.curriculum.application.acl;

import com.kalibra.api.curriculum.domain.model.aggregates.Course;
import com.kalibra.api.curriculum.domain.model.queries.GetCourseByIdQuery;
import com.kalibra.api.curriculum.domain.model.queries.GetCoursesByHolderIdQuery;
import com.kalibra.api.curriculum.domain.model.valueobjects.CourseId;
import com.kalibra.api.curriculum.domain.services.CourseQueryService;
import com.kalibra.api.curriculum.interfaces.acl.CurriculumContextFacade;
import com.kalibra.api.shared.contracts.curriculum.CourseSummary;
import com.kalibra.api.shared.contracts.curriculum.SubtopicSummary;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class CurriculumContextFacadeImpl implements CurriculumContextFacade {

    private final CourseQueryService courseQueryService;

    public CurriculumContextFacadeImpl(CourseQueryService courseQueryService) {
        this.courseQueryService = courseQueryService;
    }

    @Override
    public boolean isCourseOwnedBy(UUID courseId, String holderId) {
        return findCourse(courseId).map(course -> course.isOwnedBy(holderId)).orElse(false);
    }

    @Override
    public Optional<CourseSummary> fetchCourseSummary(UUID courseId) {
        return findCourse(courseId).map(this::toSummary);
    }

    @Override
    public List<CourseSummary> fetchCoursesByHolderId(String holderId) {
        return courseQueryService.handle(new GetCoursesByHolderIdQuery(holderId)).stream()
                .map(this::toSummary)
                .toList();
    }

    @Override
    public List<SubtopicSummary> fetchSubtopics(UUID courseId) {
        return findCourse(courseId)
                .map(course -> course.getSubtopics().stream()
                        .map(subtopic -> new SubtopicSummary(
                                subtopic.getId().value(), subtopic.getName(), subtopic.getDisplayOrder()))
                        .toList())
                .orElse(List.of());
    }

    private Optional<Course> findCourse(UUID courseId) {
        if (courseId == null) {
            return Optional.empty();
        }
        return courseQueryService.handle(new GetCourseByIdQuery(new CourseId(courseId)));
    }

    private CourseSummary toSummary(Course course) {
        return new CourseSummary(course.getId().value(), course.getName(), course.getCode().value(), course.getHolderId());
    }
}
