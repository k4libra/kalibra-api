package com.kalibra.api.progress.interfaces.rest;

import com.kalibra.api.progress.domain.exceptions.ExerciseNotFoundException;
import com.kalibra.api.progress.domain.services.ExerciseAttemptCommandService;
import com.kalibra.api.progress.interfaces.rest.resources.PracticeExerciseResource;
import com.kalibra.api.progress.interfaces.rest.resources.RequestPracticeExerciseResource;
import com.kalibra.api.progress.interfaces.rest.transform.ExerciseAttemptAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@Tag(name = "Practice Exercises", description = "Adaptive exercises delivered to the student, already verified")
@RequestMapping("/api/v1/practice-exercises")
public class PracticeExercisesController {

    private static final String NO_EXERCISE = "No verified exercise is available for the subtopic yet";

    private final ExerciseAttemptCommandService exerciseAttemptCommandService;
    private final ExerciseAttemptAssembler exerciseAttemptAssembler;

    public PracticeExercisesController(
            ExerciseAttemptCommandService exerciseAttemptCommandService,
            ExerciseAttemptAssembler exerciseAttemptAssembler) {
        this.exerciseAttemptCommandService = exerciseAttemptCommandService;
        this.exerciseAttemptAssembler = exerciseAttemptAssembler;
    }

    @Operation(summary = "Request a practice exercise",
            description = "Generates an exercise for a subtopic of a course I am enrolled in, adjusted to my current mastery. "
                    + "Only an exercise approved by the verification is delivered, with its four options in order A, B, C, D "
                    + "and without the answer.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Verified exercise ready to be answered"),
            @ApiResponse(responseCode = "400", description = "Missing course or subtopic", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "The authenticated user is not a student, or is not enrolled in the course", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "The subtopic has no ready material or no exercise passed the verification", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT cookie", content = @Content)
    })
    @PostMapping
    public ResponseEntity<PracticeExerciseResource> request(
            @Valid @RequestBody RequestPracticeExerciseResource resource,
            Authentication authentication) {
        var command = exerciseAttemptAssembler.toCommand(resource, authentication.getName());
        var exercise = exerciseAttemptCommandService.handle(command)
                .map(exerciseAttemptAssembler::toResource)
                .orElseThrow(() -> new ExerciseNotFoundException(NO_EXERCISE));
        return ResponseEntity
                .created(URI.create("/api/v1/practice-exercises/" + exercise.exerciseId()))
                .body(exercise);
    }
}
