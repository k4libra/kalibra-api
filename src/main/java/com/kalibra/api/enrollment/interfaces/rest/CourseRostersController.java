package com.kalibra.api.enrollment.interfaces.rest;

import com.kalibra.api.enrollment.domain.model.queries.GetEnrollmentRostersByHolderIdQuery;
import com.kalibra.api.enrollment.domain.services.EnrollmentQueryService;
import com.kalibra.api.enrollment.interfaces.rest.resources.CourseRosterResource;
import com.kalibra.api.enrollment.interfaces.rest.resources.RosterStudentResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/course-rosters")
public class CourseRostersController {

    private final EnrollmentQueryService enrollmentQueryService;

    public CourseRostersController(
            EnrollmentQueryService enrollmentQueryService
    ) {
        this.enrollmentQueryService = enrollmentQueryService;
    }

    @GetMapping
    public ResponseEntity<List<CourseRosterResource>> getAll(
            @RequestHeader("X-Holder-Id") String holderId
    ) {
        var groups = enrollmentQueryService.handle(
                new GetEnrollmentRostersByHolderIdQuery(holderId)
        );

        var resources = groups.stream()
                .map(group -> new CourseRosterResource(
                        group.courseId(),
                        group.courseName(),
                        group.courseCode(),
                        group.enrolledCount(),
                        group.students().stream()
                                .map(student -> new RosterStudentResource(
                                        student.studentId(),
                                        student.email(),
                                        student.enrolledAt()
                                ))
                                .toList()
                ))
                .toList();

        return ResponseEntity.ok(resources);
    }
}