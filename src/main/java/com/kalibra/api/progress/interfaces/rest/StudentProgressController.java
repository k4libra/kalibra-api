package com.kalibra.api.progress.interfaces.rest;

import com.kalibra.api.progress.domain.model.queries.GetStudentProgressByCourseQuery;
import com.kalibra.api.progress.domain.model.queries.GetStudentProgressForTeacherQuery;
import com.kalibra.api.progress.domain.model.valueobjects.CourseId;
import com.kalibra.api.progress.domain.model.valueobjects.StudentId;
import com.kalibra.api.progress.domain.services.SubtopicMasteryQueryService;
import com.kalibra.api.progress.interfaces.rest.resources.StudentProgressResource;
import com.kalibra.api.progress.interfaces.rest.transform.SubtopicMasteryAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@Tag(name = "Student Progress", description = "Progress of a student in a course, subtopic by subtopic")
@RequestMapping("/api/v1/courses/{id}/student-progress")
public class StudentProgressController {

    private static final String SUMMARY = "Get the progress of a student in a course";
    private static final String DESCRIPTION = "Mastery and solved exercises per subtopic, plus the explanations of the latest answers. "
            + "Without studentId it is the progress of the authenticated student, who must be enrolled. "
            + "With studentId it is the progress of that enrolled student, for the teacher who owns the course. "
            + "A student without solved exercises answers hasActivity = false and every subtopic at NO_DATA.";

    private final SubtopicMasteryQueryService queryService;
    private final SubtopicMasteryAssembler assembler;

    public StudentProgressController(
            SubtopicMasteryQueryService queryService,
            SubtopicMasteryAssembler assembler) {
        this.queryService = queryService;
        this.assembler = assembler;
    }

    // Both handlers share one documented operation: OpenAPI has a single GET per path.
    @Operation(summary = SUMMARY, description = DESCRIPTION)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Progress of the student in the course"),
            @ApiResponse(responseCode = "400", description = "Malformed or empty studentId", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "The student is not enrolled in the course, or the user is neither a student nor a teacher", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "With studentId: the course does not exist or belongs to another teacher", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT cookie", content = @Content)
    })
    @GetMapping(params = "!studentId")
    public ResponseEntity<StudentProgressResource> getMine(
            @PathVariable("id") UUID courseId,
            Authentication authentication) {
        var query = new GetStudentProgressByCourseQuery(
                authentication.getName(),
                new CourseId(courseId)
        );
        return ResponseEntity.ok(assembler.toResource(queryService.handle(query)));
    }

    // Both handlers share one documented operation: OpenAPI has a single GET per path.
    @Operation(summary = SUMMARY, description = DESCRIPTION)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Progress of the student in the course"),
            @ApiResponse(responseCode = "400", description = "Malformed or empty studentId", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "The student is not enrolled in the course, or the user is neither a student nor a teacher", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "With studentId: the course does not exist or belongs to another teacher", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT cookie", content = @Content)
    })
    @GetMapping(params = "studentId")
    public ResponseEntity<StudentProgressResource> getByStudent(
            @PathVariable("id") UUID courseId,
            @Parameter(description = "Only for teachers: the enrolled student whose progress to see")
            @RequestParam(required = false) UUID studentId,
            Authentication authentication) {
        if (studentId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "studentId cannot be empty");
        }
        var query = new GetStudentProgressForTeacherQuery(
                authentication.getName(),
                new CourseId(courseId),
                new StudentId(studentId)
        );
        return ResponseEntity.ok(assembler.toResource(queryService.handle(query)));
    }
}
