package com.kalibra.api.enrollment.interfaces.rest;

import com.kalibra.api.enrollment.domain.model.queries.GetEnrolledCoursesByStudentQuery;
import com.kalibra.api.enrollment.domain.model.valueobjects.StudentId;
import com.kalibra.api.enrollment.domain.services.EnrollmentQueryService;
import com.kalibra.api.enrollment.interfaces.rest.resources.EnrolledCourseResource;
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
import java.util.UUID;

@RestController
@Tag(name = "Student Courses", description = "Courses the authenticated student is enrolled in")
@RequestMapping("/api/v1/students/me/courses")
public class StudentCoursesController {

    private final EnrollmentQueryService queryService;
    private final EnrollmentAssembler assembler;

    public StudentCoursesController(EnrollmentQueryService queryService, EnrollmentAssembler assembler) {
        this.queryService = queryService;
        this.assembler = assembler;
    }

    @Operation(summary = "List my enrolled courses",
            description = "Returns every course the authenticated student is enrolled in, with its teacher and subtopics.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Enrolled courses"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT cookie", content = @Content),
            @ApiResponse(responseCode = "403", description = "The authenticated user is not a student", content = @Content)
    })
    @GetMapping
    public ResponseEntity<List<EnrolledCourseResource>> getMine(Authentication authentication) {
        var studentId = new StudentId(UUID.fromString(authentication.getName()));
        var courses = queryService.handle(new GetEnrolledCoursesByStudentQuery(studentId));
        return ResponseEntity.ok(courses.stream().map(assembler::toResource).toList());
    }
}
