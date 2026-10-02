package com.kalibra.api.progress.interfaces.rest;

import com.kalibra.api.progress.domain.model.queries.GetPracticeSubtopicsByCourseQuery;
import com.kalibra.api.progress.domain.model.valueobjects.CourseId;
import com.kalibra.api.progress.domain.services.SubtopicMasteryQueryService;
import com.kalibra.api.progress.interfaces.rest.resources.SubtopicMasteryResource;
import com.kalibra.api.progress.interfaces.rest.transform.SubtopicMasteryAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@Tag(name = "Subtopic Masteries", description = "Subtopics of a course the student can practice, with their mastery level")
@RequestMapping("/api/v1/subtopic-masteries")
public class SubtopicMasteriesController {

    private final SubtopicMasteryQueryService queryService;
    private final SubtopicMasteryAssembler assembler;

    public SubtopicMasteriesController(
            SubtopicMasteryQueryService queryService,
            SubtopicMasteryAssembler assembler) {
        this.queryService = queryService;
        this.assembler = assembler;
    }

    @Operation(summary = "List the subtopics of a course I am enrolled in",
            description = "Returns the subtopics available to practice, in display order, each with my mastery level "
                    + "(NO_DATA until I answer an exercise of it).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Subtopics of the course"),
            @ApiResponse(responseCode = "400", description = "Missing or malformed courseId", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "The authenticated user is not a student, or is not enrolled in the course", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT cookie", content = @Content)
    })
    @GetMapping
    public ResponseEntity<List<SubtopicMasteryResource>> getAll(
            @RequestParam UUID courseId,
            Authentication authentication) {
        var query = new GetPracticeSubtopicsByCourseQuery(
                authentication.getName(),
                new CourseId(courseId)
        );
        return ResponseEntity.ok(queryService.handle(query)
                .stream()
                .map(assembler::toResource)
                .toList());
    }
}
