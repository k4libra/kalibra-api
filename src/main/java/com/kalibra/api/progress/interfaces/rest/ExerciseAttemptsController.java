package com.kalibra.api.progress.interfaces.rest;

import com.kalibra.api.progress.domain.model.queries.GetAttemptHistoryByCourseQuery;
import com.kalibra.api.progress.domain.model.valueobjects.AttemptResultFilter;
import com.kalibra.api.progress.domain.model.valueobjects.CourseId;
import com.kalibra.api.progress.domain.model.valueobjects.Pagination;
import com.kalibra.api.progress.domain.services.ExerciseAttemptCommandService;
import com.kalibra.api.progress.domain.services.ExerciseAttemptQueryService;
import com.kalibra.api.progress.interfaces.rest.resources.AttemptHistoryResource;
import com.kalibra.api.progress.interfaces.rest.resources.ExerciseAttemptResource;
import com.kalibra.api.progress.interfaces.rest.resources.SubmitAnswerResource;
import com.kalibra.api.progress.interfaces.rest.transform.ExerciseAttemptAssembler;
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
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@Tag(name = "Exercise Attempts", description = "Answers of the student to practice exercises and their history")
@RequestMapping("/api/v1/exercise-attempts")
public class ExerciseAttemptsController {

    private final ExerciseAttemptCommandService commandService;
    private final ExerciseAttemptQueryService queryService;
    private final ExerciseAttemptAssembler assembler;

    public ExerciseAttemptsController(
            ExerciseAttemptCommandService commandService,
            ExerciseAttemptQueryService queryService,
            ExerciseAttemptAssembler assembler) {
        this.commandService = commandService;
        this.queryService = queryService;
        this.assembler = assembler;
    }

    @Operation(summary = "Answer a practice exercise",
            description = "Records the answer of the authenticated student, tells whether it was correct with its explanation, "
                    + "and returns how the mastery of the subtopic changed. The first answer in a subtopic starts from the base mastery.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Answer recorded, with the result and the mastery change"),
            @ApiResponse(responseCode = "400", description = "Missing course or exercise, or an option other than A, B, C or D", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "The authenticated user is not a student, or is not enrolled in the course", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "The exercise does not exist, was discarded or belongs to another course", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT cookie", content = @Content)
    })
    @PostMapping
    public ResponseEntity<ExerciseAttemptResource> submit(
            @Valid @RequestBody SubmitAnswerResource resource,
            Authentication authentication) {
        var attempt = commandService.handle(
                assembler.toCommand(resource, authentication.getName())
        );
        return ResponseEntity
                .created(URI.create("/api/v1/exercise-attempts/" + attempt.getId().value()))
                .body(assembler.toResource(attempt));
    }

    @Operation(summary = "List the exercises I have solved",
            description = "Paginated, newest first, with the result and date of each answer. The counts cover the whole history, "
                    + "whatever the active filter; ALL (the default) brings every answer back.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page of the history; empty content when nothing was solved yet"),
            @ApiResponse(responseCode = "400", description = "Missing courseId, unsupported result, page below 0 or size outside 1..100", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT cookie", content = @Content),
            @ApiResponse(responseCode = "403", description = "The authenticated user is not a student", content = @Content)
    })
    @GetMapping
    public ResponseEntity<AttemptHistoryResource> getAll(
            @RequestParam UUID courseId,
            @Parameter(description = "Keeps only the answers with this result; every answer when omitted",
                    schema = @Schema(allowableValues = {"ALL", "CORRECT", "INCORRECT"}))
            @RequestParam(required = false) String result,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(Pagination.MAX_SIZE) int size,
            Authentication authentication) {
        var query = new GetAttemptHistoryByCourseQuery(
                authentication.getName(),
                new CourseId(courseId),
                AttemptResultFilter.from(result),
                Pagination.of(page, size)
        );
        return ResponseEntity.ok(assembler.toResource(queryService.handle(query)));
    }
}
