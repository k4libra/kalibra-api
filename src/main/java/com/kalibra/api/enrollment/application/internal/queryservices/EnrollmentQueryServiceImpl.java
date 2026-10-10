package com.kalibra.api.enrollment.application.internal.queryservices;

import com.kalibra.api.enrollment.application.internal.outboundservices.acl.ExternalCurriculumService;
import com.kalibra.api.enrollment.application.internal.outboundservices.acl.ExternalIamService;
import com.kalibra.api.enrollment.domain.model.aggregates.Enrollment;
import com.kalibra.api.enrollment.domain.model.queries.GetEnrolledCoursesByStudentQuery;
import com.kalibra.api.enrollment.domain.model.queries.GetEnrollmentByStudentAndCourseQuery;
import com.kalibra.api.enrollment.domain.model.queries.GetEnrollmentRostersByHolderIdQuery;
import com.kalibra.api.enrollment.domain.model.queries.GetEnrollmentsByCourseQuery;
import com.kalibra.api.enrollment.domain.model.valueobjects.CourseId;
import com.kalibra.api.enrollment.domain.model.valueobjects.CourseRosterGroup;
import com.kalibra.api.enrollment.domain.model.valueobjects.EnrolledCourse;
import com.kalibra.api.enrollment.domain.model.valueobjects.RosterLine;
import com.kalibra.api.enrollment.domain.repositories.EnrollmentRepository;
import com.kalibra.api.enrollment.domain.services.EnrollmentQueryService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class EnrollmentQueryServiceImpl implements EnrollmentQueryService {

    private final EnrollmentRepository enrollmentRepository;
    private final ExternalCurriculumService externalCurriculumService;
    private final ExternalIamService externalIamService;

    public EnrollmentQueryServiceImpl(
            EnrollmentRepository enrollmentRepository,
            ExternalCurriculumService externalCurriculumService,
            ExternalIamService externalIamService
    ) {
        this.enrollmentRepository = enrollmentRepository;
        this.externalCurriculumService = externalCurriculumService;
        this.externalIamService = externalIamService;
    }

    @Override
    public List<CourseRosterGroup> handle(
            GetEnrollmentRostersByHolderIdQuery query
    ) {
        var courses = externalCurriculumService
                .fetchCoursesByHolderId(query.holderId());

        var courseIds = courses.stream()
                .map(course -> new CourseId(course.courseId()))
                .toList();

        var enrollments = enrollmentRepository.findAllByCourseIdIn(courseIds);

        var result = new ArrayList<CourseRosterGroup>();

        for (var course : courses) {

            var students = enrollments.stream()
                    .filter(enrollment ->
                            enrollment.getCourseId().value()
                                    .equals(course.courseId()))
                    .map(enrollment -> {
                        var name = externalIamService.fetchUserName(enrollment.getStudentId().value());
                        return new RosterLine(
                                enrollment.getStudentId().value(),
                                enrollment.getStudentEmail().value(),
                                name.firstName(),
                                name.lastName(),
                                enrollment.getEnrolledAt()
                        );
                    })
                    .toList();

            result.add(new CourseRosterGroup(
                    course.courseId(),
                    course.name(),
                    course.code(),
                    students.size(),
                    students
            ));
        }

        return result;
    }

    @Override
    public List<Enrollment> handle(
            GetEnrollmentsByCourseQuery query
    ) {
        return enrollmentRepository.findAllByCourseId(
                query.courseId()
        );
    }

    @Override
    public Optional<Enrollment> handle(
            GetEnrollmentByStudentAndCourseQuery query
    ) {
        return enrollmentRepository.findByStudentIdAndCourseId(
                query.studentId(),
                query.courseId()
        );
    }

    @Override
    public List<EnrolledCourse> handle(
            GetEnrolledCoursesByStudentQuery query
    ) {
        var result = new ArrayList<EnrolledCourse>();
        for (var enrollment : enrollmentRepository.findAllByStudentId(query.studentId())) {
            var course = externalCurriculumService.fetchCourseSummary(enrollment.getCourseId());
            if (course.isEmpty()) {
                continue;
            }
            var summary = course.get();
            var subtopics = externalCurriculumService.fetchSubtopics(enrollment.getCourseId())
                    .stream()
                    .map(subtopic -> new EnrolledCourse.EnrolledSubtopic(
                            subtopic.subtopicId(), subtopic.name(), subtopic.displayOrder()))
                    .toList();
            result.add(new EnrolledCourse(
                    summary.courseId(),
                    summary.name(),
                    summary.code(),
                    teacherNameOf(summary.holderId()),
                    subtopics,
                    enrollment.getEnrolledAt()
            ));
        }
        return result;
    }

    private String teacherNameOf(String holderId) {
        try {
            var name = externalIamService.fetchUserName(UUID.fromString(holderId));
            return name.firstName() == null ? null : (name.firstName() + " " + name.lastName()).strip();
        } catch (IllegalArgumentException malformedHolderId) {
            return null;
        }
    }
}
