package com.kalibra.api.curriculum.interfaces.rest;

import com.kalibra.api.curriculum.domain.model.queries.GetGeneratedExercisesByCourseQuery;
import com.kalibra.api.curriculum.domain.model.valueobjects.CourseId;
import com.kalibra.api.curriculum.domain.model.valueobjects.Pagination;
import com.kalibra.api.curriculum.domain.model.valueobjects.SubtopicId;
import com.kalibra.api.curriculum.domain.services.GeneratedExerciseCommandService;
import com.kalibra.api.curriculum.domain.services.GeneratedExerciseQueryService;
import com.kalibra.api.curriculum.interfaces.rest.resources.GenerateExerciseResource;
import com.kalibra.api.curriculum.interfaces.rest.resources.GeneratedExercisePageResource;
import com.kalibra.api.curriculum.interfaces.rest.resources.GeneratedExerciseResource;
import com.kalibra.api.curriculum.interfaces.rest.transform.GeneratedExerciseAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@Tag(name = "Generated Exercises", description = "Exercises generated from the curricular material and their verification result")
@RequestMapping("/api/v1/courses/{id}/generated-exercises")
public class GeneratedExercisesController {

    private final GeneratedExerciseCommandService commandService;
    private final GeneratedExerciseQueryService queryService;
    private final GeneratedExerciseAssembler assembler;

    public GeneratedExercisesController(GeneratedExerciseCommandService commandService,
                                        GeneratedExerciseQueryService queryService,
                                        GeneratedExerciseAssembler assembler) {
        this.commandService = commandService;
        this.queryService = queryService;
        this.assembler = assembler;
    }

    @Operation(summary = "Generate exercises for a subtopic",
            description = "Generates new exercises anchored to the ready curricular material of a subtopic of my course. "
                    + "Every attempt is verified and returned, approved or discarded with its rejection reason.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Exercises generated and verified"),
            @ApiResponse(responseCode = "400", description = "Missing subtopic, quantity outside 1..10 or a subtopic that does not belong to the course", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "The course does not exist or belongs to another teacher", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "The subtopic has no curricular material ready yet", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT cookie", content = @Content),
            @ApiResponse(responseCode = "403", description = "The authenticated user is not a teacher", content = @Content)
    })
    @PostMapping
    public ResponseEntity<List<GeneratedExerciseResource>> generate(@PathVariable("id") UUID courseId,
                                                                    @Valid @RequestBody GenerateExerciseResource resource,
                                                                    Authentication authentication) {
        var command = assembler.toCommand(resource, courseId, authentication.getName());
        var exercises = commandService.handle(command);
        return new ResponseEntity<>(exercises.stream().map(assembler::toResource).toList(), HttpStatus.CREATED);
    }

    @Operation(summary = "List the generated exercises of my course",
            description = "Paginated, newest first, with the content of each exercise and its verification result. "
                    + "Discarded exercises come with their rejection reason.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page of generated exercises"),
            @ApiResponse(responseCode = "400", description = "page below 0 or size outside 1..100", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "The course does not exist or belongs to another teacher", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT cookie", content = @Content),
            @ApiResponse(responseCode = "403", description = "The authenticated user is not a teacher", content = @Content)
    })
    @GetMapping
    public ResponseEntity<GeneratedExercisePageResource> getAll(@PathVariable("id") UUID courseId,
                                                                @Parameter(description = "Keeps only the exercises of this subtopic")
                                                                @RequestParam(required = false) UUID subtopicId,
                                                                @RequestParam(defaultValue = "0") @Min(0) int page,
                                                                @RequestParam(defaultValue = "20") @Min(1) @Max(Pagination.MAX_SIZE) int size,
                                                                Authentication authentication) {
        var query = new GetGeneratedExercisesByCourseQuery(authentication.getName(), new CourseId(courseId),
                Optional.ofNullable(subtopicId).map(SubtopicId::new), Pagination.of(page, size));
        return ResponseEntity.ok(assembler.toResource(queryService.handle(query)));
    }
}
