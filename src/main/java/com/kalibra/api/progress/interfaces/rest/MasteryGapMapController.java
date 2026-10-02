package com.kalibra.api.progress.interfaces.rest;

import com.kalibra.api.progress.domain.model.queries.GetMasteryGapMapByCourseQuery;
import com.kalibra.api.progress.domain.model.valueobjects.CourseId;
import com.kalibra.api.progress.domain.services.SubtopicMasteryQueryService;
import com.kalibra.api.progress.interfaces.rest.resources.MasteryGapMapResource;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@Tag(name = "Mastery Gap Map", description = "Mastery of the group per subtopic, to decide what to reinforce")
@RequestMapping("/api/v1/courses/{id}/mastery-gap-map")
public class MasteryGapMapController {

    private final SubtopicMasteryQueryService queryService;
    private final SubtopicMasteryAssembler assembler;

    public MasteryGapMapController(
            SubtopicMasteryQueryService queryService,
            SubtopicMasteryAssembler assembler) {
        this.queryService = queryService;
        this.assembler = assembler;
    }

    @Operation(summary = "Get the mastery gap map of my course",
            description = "Per subtopic: the group mastery, how many students are at each level (low, medium, high or no data) "
                    + "and its reinforcement priority; plus the mastery of each enrolled student in each subtopic. "
                    + "hasSufficientData is false while there are no estimates to show.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Mastery gap map of the course"),
            @ApiResponse(responseCode = "404", description = "The course does not exist or belongs to another teacher", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT cookie", content = @Content),
            @ApiResponse(responseCode = "403", description = "The authenticated user is not a teacher", content = @Content)
    })
    @GetMapping
    public ResponseEntity<MasteryGapMapResource> get(
            @PathVariable("id") UUID courseId,
            Authentication authentication) {
        var query = new GetMasteryGapMapByCourseQuery(
                authentication.getName(),
                new CourseId(courseId)
        );
        return ResponseEntity.ok(assembler.toResource(queryService.handle(query)));
    }
}
