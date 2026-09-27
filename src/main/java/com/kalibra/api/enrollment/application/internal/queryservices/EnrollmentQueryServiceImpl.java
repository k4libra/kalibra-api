package com.kalibra.api.enrollment.application.internal.queryservices;

import com.kalibra.api.enrollment.application.internal.outboundservices.acl.ExternalCurriculumService;
import com.kalibra.api.enrollment.domain.model.aggregates.Enrollment;
import com.kalibra.api.enrollment.domain.model.queries.GetEnrollmentByStudentAndCourseQuery;
import com.kalibra.api.enrollment.domain.model.queries.GetEnrollmentRostersByHolderIdQuery;
import com.kalibra.api.enrollment.domain.model.queries.GetEnrollmentsByCourseQuery;
import com.kalibra.api.enrollment.domain.model.valueobjects.CourseId;
import com.kalibra.api.enrollment.domain.model.valueobjects.CourseRosterGroup;
import com.kalibra.api.enrollment.domain.model.valueobjects.RosterLine;
import com.kalibra.api.enrollment.domain.repositories.EnrollmentRepository;
import com.kalibra.api.enrollment.domain.services.EnrollmentQueryService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class EnrollmentQueryServiceImpl implements EnrollmentQueryService {

    private final EnrollmentRepository enrollmentRepository;
    private final ExternalCurriculumService externalCurriculumService;

    public EnrollmentQueryServiceImpl(
            EnrollmentRepository enrollmentRepository,
            ExternalCurriculumService externalCurriculumService
    ) {
        this.enrollmentRepository = enrollmentRepository;
        this.externalCurriculumService = externalCurriculumService;
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
                    .map(enrollment -> new RosterLine(
                            enrollment.getStudentId().value(),
                            enrollment.getStudentEmail().value(),
                            enrollment.getEnrolledAt()
                    ))
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
}