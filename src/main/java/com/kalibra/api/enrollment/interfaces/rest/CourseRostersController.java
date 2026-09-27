package com.kalibra.api.enrollment.interfaces.rest;

import com.kalibra.api.enrollment.domain.model.queries.GetEnrollmentRostersByHolderIdQuery;
import com.kalibra.api.enrollment.domain.services.EnrollmentQueryService;
import com.kalibra.api.enrollment.interfaces.rest.resources.CourseRosterResource;
import com.kalibra.api.enrollment.interfaces.rest.transform.EnrollmentAssembler;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/course-rosters")
public class CourseRostersController {

    private final EnrollmentQueryService enrollmentQueryService;
    private final EnrollmentAssembler enrollmentAssembler;

    public CourseRostersController(
            EnrollmentQueryService enrollmentQueryService,
            EnrollmentAssembler enrollmentAssembler
    ) {
        this.enrollmentQueryService = enrollmentQueryService;
        this.enrollmentAssembler = enrollmentAssembler;
    }

    @GetMapping
    public ResponseEntity<List<CourseRosterResource>> getAll(
            @RequestHeader("X-Holder-Id") String holderId
    ) {
        var groups = enrollmentQueryService.handle(
                new GetEnrollmentRostersByHolderIdQuery(holderId)
        );

        var resources = groups.stream()
                .map(enrollmentAssembler::toResource)
                .toList();

        return ResponseEntity.ok(resources);
    }
}