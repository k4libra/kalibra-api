package com.kalibra.api.enrollment.interfaces.rest;

import com.kalibra.api.enrollment.domain.model.queries.GetEnrollmentRostersByHolderIdQuery;
import com.kalibra.api.enrollment.domain.services.EnrollmentQueryService;
import com.kalibra.api.enrollment.interfaces.rest.resources.CourseRosterResource;
import com.kalibra.api.enrollment.interfaces.rest.transform.EnrollmentAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Tag(name = "Course Rosters", description = "Students enrolled in the courses of the teacher")
@RequestMapping("/api/v1/course-rosters")
public class CourseRostersController {

    private final EnrollmentQueryService queryService;
    private final EnrollmentAssembler assembler;

    public CourseRostersController(EnrollmentQueryService queryService, EnrollmentAssembler assembler) {
        this.queryService = queryService;
        this.assembler = assembler;
    }

    @Operation(summary = "List my enrolled students by course",
            description = "Returns every course of the authenticated teacher with its name, code, enrolled count and students. A course without students comes with an empty list.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Enrolled students grouped by course"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT cookie", content = @Content),
            @ApiResponse(responseCode = "403", description = "The authenticated user is not a teacher", content = @Content)
    })
    @GetMapping
    public ResponseEntity<List<CourseRosterResource>> getAll(Authentication authentication) {
        var groups = queryService.handle(new GetEnrollmentRostersByHolderIdQuery(authentication.getName()));
        return ResponseEntity.ok(groups.stream().map(assembler::toResource).toList());
    }
}
